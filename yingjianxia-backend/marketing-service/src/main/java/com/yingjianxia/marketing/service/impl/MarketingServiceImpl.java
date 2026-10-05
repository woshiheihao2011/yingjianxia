package com.yingjianxia.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.marketing.dto.*;
import com.yingjianxia.marketing.entity.*;
import com.yingjianxia.marketing.enums.MarketingErrorCode;
import com.yingjianxia.marketing.mapper.*;
import com.yingjianxia.marketing.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销服务实现
 * <p>
 * 安全核心：
 * <ol>
 *   <li>优惠券领取 — 原子 UPDATE 防超发 + 唯一约束防重复</li>
 *   <li>积分账户 — 乐观锁 version 防并发超扣</li>
 *   <li>每日签到 — 唯一约束(user_id, signin_date) 防重复签到</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingServiceImpl implements MarketingService {

    private final CouponMapper couponMapper;
    private final CouponRecordMapper couponRecordMapper;
    private final PromotionMapper promotionMapper;
    private final PointsAccountMapper pointsAccountMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final DailySigninMapper dailySigninMapper;

    /** 乐观锁重试次数 */
    private static final int OPTIMISTIC_RETRY = 3;
    /** 积分过期来源标记 */
    private static final String SOURCE_EXPIRE = "expire";

    /* ================================================================
     *  优惠券 — 卖家端
     * ================================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon createCoupon(CouponCreateReq req, Long creatorId) {
        // 校验类型参数
        validateCouponTypeParams(req);
        // 校验时间
        if (req.getValidStart().isAfter(req.getValidEnd()) || !req.getValidStart().isAfter(LocalDateTime.now())) {
            throw new BusinessException(MarketingErrorCode.COUPON_TIME_INVALID);
        }

        Coupon c = new Coupon();
        c.setCreatorId(creatorId);
        c.setShopId(req.getShopId());
        c.setName(req.getName());
        c.setCouponType(req.getCouponType());
        c.setFaceValue(req.getFaceValue());
        c.setDiscountRate(req.getDiscountRate());
        c.setMinSpend(req.getMinSpend() == null ? java.math.BigDecimal.ZERO : req.getMinSpend());
        c.setTotalCount(req.getTotalCount());
        c.setClaimedCount(0);
        c.setUsedCount(0);
        c.setPerUserLimit(req.getPerUserLimit() == null ? 1 : req.getPerUserLimit());
        c.setScope(req.getScope());
        c.setScopeValue(req.getScopeValue());
        c.setValidStart(req.getValidStart());
        c.setValidEnd(req.getValidEnd());
        c.setStatus(Coupon.STATUS_ACTIVE);
        couponMapper.insert(c);
        log.info("【优惠券创建】id={}, name={}, creator={}, total={}", c.getId(), c.getName(), creatorId, c.getTotalCount());
        return c;
    }

    @Override
    public void pauseCoupon(Long couponId, Long sellerId) {
        Coupon c = requireCoupon(couponId);
        if (!c.getCreatorId().equals(sellerId)) {
            throw new BusinessException(MarketingErrorCode.COUPON_NO_PERMISSION);
        }
        if (c.getStatus() != Coupon.STATUS_ACTIVE) {
            throw new BusinessException(MarketingErrorCode.COUPON_NOT_ACTIVE);
        }
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .set(Coupon::getStatus, Coupon.STATUS_PAUSED)
                .set(Coupon::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void endCoupon(Long couponId, Long sellerId) {
        Coupon c = requireCoupon(couponId);
        if (!c.getCreatorId().equals(sellerId)) {
            throw new BusinessException(MarketingErrorCode.COUPON_NO_PERMISSION);
        }
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .set(Coupon::getStatus, Coupon.STATUS_ENDED)
                .set(Coupon::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public List<Coupon> listCouponsByShop(Long shopId) {
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getShopId, shopId)
                .orderByDesc(Coupon::getCreatedAt));
    }

    /* ================================================================
     *  优惠券 — 买家端
     * ================================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponClaimResp claimCoupon(Long couponId, Long userId) {
        // 1. 校验优惠券
        Coupon coupon = requireCoupon(couponId);
        if (coupon.getStatus() != Coupon.STATUS_ACTIVE) {
            throw new BusinessException(MarketingErrorCode.COUPON_NOT_ACTIVE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidStart()) || now.isAfter(coupon.getValidEnd())) {
            throw new BusinessException(MarketingErrorCode.COUPON_EXPIRED);
        }

        // 2. 限领校验
        Long claimedCount = couponRecordMapper.selectCount(new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getCouponId, couponId)
                .eq(CouponRecord::getUserId, userId));
        if (claimedCount != null && claimedCount >= coupon.getPerUserLimit()) {
            throw new BusinessException(MarketingErrorCode.COUPON_PER_USER_LIMIT);
        }

        // 3. 原子扣减库存：UPDATE coupons SET claimed_count = claimed_count + 1
        //    WHERE id = ? AND claimed_count < total_count（防超发）
        int rows = couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .apply("claimed_count < total_count")
                .setSql("claimed_count = claimed_count + 1"));
        if (rows == 0) {
            throw new BusinessException(MarketingErrorCode.COUPON_STOCK_EMPTY);
        }

        // 4. 插入领券记录（uk_coupon_user 唯一约束防重复领取）
        CouponRecord record = new CouponRecord();
        record.setCouponId(couponId);
        record.setUserId(userId);
        record.setStatus(CouponRecord.STATUS_UNUSED);
        record.setClaimedAt(now);
        record.setExpireAt(coupon.getValidEnd());
        try {
            couponRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复领取 → 回补库存
            couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                    .eq(Coupon::getId, couponId)
                    .setSql("claimed_count = claimed_count - 1"));
            throw new BusinessException(MarketingErrorCode.COUPON_ALREADY_CLAIMED);
        }

        // 5. 构建响应
        CouponClaimResp resp = new CouponClaimResp();
        resp.setRecordId(record.getId());
        resp.setCouponId(coupon.getId());
        resp.setCouponName(coupon.getName());
        resp.setCouponType(coupon.getCouponType());
        resp.setFaceValue(coupon.getFaceValue());
        resp.setDiscountRate(coupon.getDiscountRate());
        resp.setMinSpend(coupon.getMinSpend());
        resp.setExpireAt(coupon.getValidEnd());
        log.info("【优惠券领取】couponId={}, userId={}, recordId={}", couponId, userId, record.getId());
        return resp;
    }

    @Override
    public List<CouponRecord> listMyCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<CouponRecord> wrapper = new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getUserId, userId)
                .orderByDesc(CouponRecord::getClaimedAt);
        if (status != null) {
            wrapper.eq(CouponRecord::getStatus, status);
        }
        return couponRecordMapper.selectList(wrapper);
    }

    @Override
    public List<CouponRecord> listAvailableCoupons(Long userId, java.math.BigDecimal orderAmount) {
        List<CouponRecord> unused = couponRecordMapper.selectList(new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getUserId, userId)
                .eq(CouponRecord::getStatus, CouponRecord.STATUS_UNUSED));
        if (unused.isEmpty()) return unused;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return unused.stream().filter(r -> {
            Coupon c = couponMapper.selectById(r.getCouponId());
            if (c == null) return false;
            if (c.getStatus() != null && c.getStatus() != 1) return false;
            if (c.getMinSpend() != null && orderAmount != null
                    && orderAmount.compareTo(c.getMinSpend()) < 0) return false;
            if (c.getValidStart() != null && now.isBefore(c.getValidStart())) return false;
            if (c.getValidEnd() != null && now.isAfter(c.getValidEnd())) return false;
            return true;
        }).collect(java.util.stream.Collectors.toList());
    }

    /* ================================================================
     *  优惠券 — 内部调用（订单服务）
     * ================================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(CouponUseReq req, Long userId) {
        CouponRecord record = couponRecordMapper.selectById(req.getCouponRecordId());
        if (record == null) {
            throw new BusinessException(MarketingErrorCode.COUPON_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(MarketingErrorCode.COUPON_NO_PERMISSION);
        }
        if (record.getStatus() == CouponRecord.STATUS_USED) {
            throw new BusinessException(MarketingErrorCode.COUPON_USED);
        }
        if (record.getStatus() == CouponRecord.STATUS_EXPIRED) {
            throw new BusinessException(MarketingErrorCode.COUPON_EXPIRED);
        }

        Coupon coupon = requireCoupon(record.getCouponId());
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(coupon.getValidEnd())) {
            throw new BusinessException(MarketingErrorCode.COUPON_EXPIRED);
        }

        // 使用门槛校验
        if (coupon.getMinSpend() != null && req.getOrderAmount().compareTo(coupon.getMinSpend()) < 0) {
            throw new BusinessException(MarketingErrorCode.COUPON_MIN_SPEND_NOT_MET);
        }

        // 乐观标记已使用（WHERE status = UNUSED 防并发使用）
        int rows = couponRecordMapper.update(null, new LambdaUpdateWrapper<CouponRecord>()
                .eq(CouponRecord::getId, req.getCouponRecordId())
                .eq(CouponRecord::getStatus, CouponRecord.STATUS_UNUSED)
                .set(CouponRecord::getStatus, CouponRecord.STATUS_USED)
                .set(CouponRecord::getUsedOrderId, req.getOrderId())
                .set(CouponRecord::getUsedAt, now));
        if (rows == 0) {
            throw new BusinessException(MarketingErrorCode.COUPON_USED);
        }

        // 递增已使用数
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, record.getCouponId())
                .setSql("used_count = used_count + 1"));
        log.info("【优惠券核销】recordId={}, orderId={}, userId={}", req.getCouponRecordId(), req.getOrderId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundCoupon(Long couponRecordId, Long orderId) {
        CouponRecord record = couponRecordMapper.selectById(couponRecordId);
        if (record == null) {
            throw new BusinessException(MarketingErrorCode.COUPON_RECORD_NOT_FOUND);
        }
        if (record.getStatus() != CouponRecord.STATUS_USED) {
            throw new BusinessException(MarketingErrorCode.COUPON_REFUND_FAIL);
        }
        if (record.getUsedOrderId() == null || !record.getUsedOrderId().equals(orderId)) {
            throw new BusinessException(MarketingErrorCode.COUPON_REFUND_FAIL);
        }

        // 退还：状态→未使用
        couponRecordMapper.update(null, new LambdaUpdateWrapper<CouponRecord>()
                .eq(CouponRecord::getId, couponRecordId)
                .eq(CouponRecord::getStatus, CouponRecord.STATUS_USED)
                .set(CouponRecord::getStatus, CouponRecord.STATUS_UNUSED)
                .set(CouponRecord::getUsedOrderId, null)
                .set(CouponRecord::getUsedAt, null));

        // 递减已使用数
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, record.getCouponId())
                .setSql("used_count = used_count - 1"));
        log.info("【优惠券退还】recordId={}, orderId={}", couponRecordId, orderId);
    }

    /* ================================================================
     *  积分 — 账户管理
     * ================================================================ */

    @Override
    public PointsAccount initPointsAccount(Long userId) {
        PointsAccount existing = pointsAccountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>()
                .eq(PointsAccount::getUserId, userId));
        if (existing != null) return existing;

        PointsAccount account = new PointsAccount();
        account.setUserId(userId);
        account.setTotalPoints(0);
        account.setAvailablePoints(0);
        account.setUsedPoints(0);
        account.setExpiredPoints(0);
        try {
            pointsAccountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            // uk_user_id 并发初始化 → 返回已有
            return pointsAccountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>()
                    .eq(PointsAccount::getUserId, userId));
        }
        log.info("【积分账户初始化】userId={}", userId);
        return account;
    }

    @Override
    public PointsQueryResp queryPoints(Long userId) {
        PointsAccount account = getOrCreateAccount(userId);
        PointsQueryResp resp = new PointsQueryResp();
        resp.setUserId(account.getUserId());
        resp.setTotalPoints(account.getTotalPoints());
        resp.setAvailablePoints(account.getAvailablePoints());
        resp.setUsedPoints(account.getUsedPoints());
        resp.setExpiredPoints(account.getExpiredPoints());
        return resp;
    }

    @Override
    public IPage<PointsRecord> listPointsRecords(Long userId, int pageNum, int pageSize) {
        Page<PointsRecord> page = new Page<>(pageNum, pageSize);
        return pointsRecordMapper.selectPage(page, new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId)
                .orderByDesc(PointsRecord::getCreatedAt));
    }

    /* ================================================================
     *  积分 — 增加与扣减（乐观锁 version 防并发）
     * ================================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, int amount, String source, Long relatedId, String remark) {
        if (amount <= 0) {
            throw new BusinessException(MarketingErrorCode.POINTS_REWARD_AMOUNT_INVALID);
        }

        for (int i = 0; i < OPTIMISTIC_RETRY; i++) {
            PointsAccount account = getOrCreateAccount(userId);
            int before = account.getAvailablePoints();
            account.setTotalPoints(account.getTotalPoints() + amount);
            account.setAvailablePoints(account.getAvailablePoints() + amount);
            // updateById 自动带 WHERE version = ? 并 version + 1（@Version 注解）
            int rows = pointsAccountMapper.updateById(account);
            if (rows > 0) {
                insertPointsRecord(userId, PointsRecord.TYPE_INCOME, amount, source, relatedId, before, before + amount, remark, null);
                log.info("【积分增加】userId={}, amount={}, source={}, balance {}→{}", userId, amount, source, before, before + amount);
                return;
            }
            log.warn("【积分增加乐观锁冲突】userId={}, retry={}", userId, i + 1);
        }
        throw new BusinessException("积分账户并发更新失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, int amount, Long orderId, String remark) {
        if (amount <= 0) {
            throw new BusinessException(MarketingErrorCode.POINTS_DEDUCT_AMOUNT_INVALID);
        }

        for (int i = 0; i < OPTIMISTIC_RETRY; i++) {
            PointsAccount account = getOrCreateAccount(userId);
            if (account.getAvailablePoints() < amount) {
                throw new BusinessException(MarketingErrorCode.POINTS_INSUFFICIENT);
            }
            int before = account.getAvailablePoints();
            account.setAvailablePoints(account.getAvailablePoints() - amount);
            account.setUsedPoints(account.getUsedPoints() + amount);
            int rows = pointsAccountMapper.updateById(account);
            if (rows > 0) {
                insertPointsRecord(userId, PointsRecord.TYPE_EXPENSE, amount, PointsRecord.SOURCE_DEDUCTION, orderId, before, before - amount, remark, null);
                log.info("【积分扣减】userId={}, amount={}, orderId={}, balance {}→{}", userId, amount, orderId, before, before - amount);
                return;
            }
            log.warn("【积分扣减乐观锁冲突】userId={}, retry={}", userId, i + 1);
        }
        throw new BusinessException("积分账户并发更新失败，请重试");
    }

    /* ================================================================
     *  每日签到
     * ================================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SigninResp dailySignin(Long userId) {
        LocalDate today = LocalDate.now();

        // 唯一约束 uk_user_date 防重复签到
        DailySignin existing = dailySigninMapper.selectOne(new LambdaQueryWrapper<DailySignin>()
                .eq(DailySignin::getUserId, userId)
                .eq(DailySignin::getSigninDate, today));
        if (existing != null) {
            throw new BusinessException(MarketingErrorCode.POINTS_SIGNIN_DUPLICATE);
        }

        // 计算连续天数：查昨天是否签到
        DailySignin yesterday = dailySigninMapper.selectOne(new LambdaQueryWrapper<DailySignin>()
                .eq(DailySignin::getUserId, userId)
                .eq(DailySignin::getSigninDate, today.minusDays(1)));
        int continuousDays = (yesterday != null) ? yesterday.getContinuousDays() + 1 : 1;

        // 积分：基础10 + 连续签到奖励（每天+5，上限+30，即第7天及以后得40分）
        int pointsEarned = DailySignin.BASE_POINTS + Math.min(continuousDays - 1, 6) * 5;

        // 写签到记录
        DailySignin signin = new DailySignin();
        signin.setUserId(userId);
        signin.setSigninDate(today);
        signin.setPointsEarned(pointsEarned);
        signin.setContinuousDays(continuousDays);
        dailySigninMapper.insert(signin);

        // 增加积分
        addPoints(userId, pointsEarned, PointsRecord.SOURCE_DAILY_SIGNIN, signin.getId(), "每日签到奖励");

        // 构建响应（重新查账户获取最新可用积分）
        PointsAccount account = getOrCreateAccount(userId);
        SigninResp resp = new SigninResp();
        resp.setSigninDate(today);
        resp.setPointsEarned(pointsEarned);
        resp.setContinuousDays(continuousDays);
        resp.setAvailablePoints(account.getAvailablePoints());
        log.info("【每日签到】userId={}, continuous={}, points={}", userId, continuousDays, pointsEarned);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object redeemPoints(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(MarketingErrorCode.COUPON_NOT_FOUND);
        }
        // 简化：兑换消耗 100 积分（实际应根据券面值配置）
        int cost = 100;
        deductPoints(userId, cost, null, "积分兑换优惠券");
        return claimCoupon(couponId, userId);
    }

    /* ================================================================
     *  积分过期处理（定时任务）
     * ================================================================ */

    @Override
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public int expirePoints() {
        // 查找需过期的收入记录：expire_at < now
        List<PointsRecord> toExpire = pointsRecordMapper.selectList(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getType, PointsRecord.TYPE_INCOME)
                .isNotNull(PointsRecord::getExpireAt)
                .lt(PointsRecord::getExpireAt, LocalDateTime.now()));

        int expired = 0;
        for (PointsRecord record : toExpire) {
            // 幂等：检查是否已处理过（source='expire' 且 related_id = 原收入记录ID）
            Long already = pointsRecordMapper.selectCount(new LambdaQueryWrapper<PointsRecord>()
                    .eq(PointsRecord::getSource, SOURCE_EXPIRE)
                    .eq(PointsRecord::getRelatedId, record.getId()));
            if (already != null && already > 0) continue;

            // 扣减积分
            PointsAccount account = getOrCreateAccount(record.getUserId());
            int deductAmount = Math.min(record.getAmount(), account.getAvailablePoints());

            if (deductAmount > 0) {
                for (int i = 0; i < OPTIMISTIC_RETRY; i++) {
                    int before = account.getAvailablePoints();
                    account.setAvailablePoints(before - deductAmount);
                    account.setExpiredPoints(account.getExpiredPoints() + deductAmount);
                    int rows = pointsAccountMapper.updateById(account);
                    if (rows > 0) {
                        insertPointsRecord(record.getUserId(), PointsRecord.TYPE_EXPENSE, deductAmount,
                                SOURCE_EXPIRE, record.getId(), before, before - deductAmount, "积分过期", null);
                        expired++;
                        break;
                    }
                    account = getOrCreateAccount(record.getUserId());
                }
            } else {
                // 可用不足，仅标记已处理（幂等）
                insertPointsRecord(record.getUserId(), PointsRecord.TYPE_EXPENSE, 0,
                        SOURCE_EXPIRE, record.getId(), 0, 0, "积分过期（可用不足，仅标记）", null);
                expired++;
            }
        }
        if (expired > 0) {
            log.info("【积分过期批次】处理 {} 条过期记录", expired);
        }
        return expired;
    }

    /* ================================================================
     *  营销活动 — 卖家端
     * ================================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Promotion createPromotion(PromotionCreateReq req, Long sellerId) {
        if (req.getStartAt().isAfter(req.getEndAt()) || !req.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(MarketingErrorCode.PROMOTION_TIME_INVALID);
        }

        Promotion p = new Promotion();
        p.setSellerId(sellerId);
        p.setShopId(req.getShopId());
        p.setType(req.getType());
        p.setName(req.getName());
        p.setDiscountRate(req.getDiscountRate());
        p.setMinSpend(req.getMinSpend());
        p.setReduceAmount(req.getReduceAmount());
        p.setProductIds(req.getProductIds());
        p.setActivityStock(req.getActivityStock());
        p.setSoldCount(0);
        p.setStartAt(req.getStartAt());
        p.setEndAt(req.getEndAt());
        // 自动判定初始状态
        LocalDateTime now = LocalDateTime.now();
        p.setStatus(now.isAfter(req.getStartAt()) ? Promotion.STATUS_ACTIVE : Promotion.STATUS_NOT_STARTED);
        promotionMapper.insert(p);
        log.info("【营销活动创建】id={}, name={}, seller={}", p.getId(), p.getName(), sellerId);
        return p;
    }

    @Override
    public void pausePromotion(Long promotionId, Long sellerId) {
        Promotion p = requirePromotion(promotionId);
        if (!p.getSellerId().equals(sellerId)) {
            throw new BusinessException(MarketingErrorCode.PROMOTION_NO_PERMISSION);
        }
        promotionMapper.update(null, new LambdaUpdateWrapper<Promotion>()
                .eq(Promotion::getId, promotionId)
                .set(Promotion::getStatus, Promotion.STATUS_PAUSED)
                .set(Promotion::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public List<Promotion> listPromotionsByShop(Long shopId) {
        return promotionMapper.selectList(new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getShopId, shopId)
                .orderByDesc(Promotion::getCreatedAt));
    }

    @Override
    public List<Promotion> listAllPromotions() {
        return promotionMapper.selectList(new LambdaQueryWrapper<Promotion>()
                .orderByDesc(Promotion::getCreatedAt));
    }

    /* ================================================================
     *  内部工具
     * ================================================================ */

    private Coupon requireCoupon(Long couponId) {
        Coupon c = couponMapper.selectById(couponId);
        if (c == null) {
            throw new BusinessException(MarketingErrorCode.COUPON_NOT_FOUND);
        }
        return c;
    }

    private Promotion requirePromotion(Long promotionId) {
        Promotion p = promotionMapper.selectById(promotionId);
        if (p == null) {
            throw new BusinessException(MarketingErrorCode.PROMOTION_NOT_FOUND);
        }
        return p;
    }

    private PointsAccount getOrCreateAccount(Long userId) {
        PointsAccount account = pointsAccountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>()
                .eq(PointsAccount::getUserId, userId));
        if (account == null) {
            return initPointsAccount(userId);
        }
        return account;
    }

    private void insertPointsRecord(Long userId, int type, int amount, String source,
                                     Long relatedId, int before, int after, String remark,
                                     LocalDateTime expireAt) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setSource(source);
        record.setRelatedId(relatedId);
        record.setBalanceBefore(before);
        record.setBalanceAfter(after);
        record.setRemark(remark);
        record.setExpireAt(expireAt);
        pointsRecordMapper.insert(record);
    }

    private void validateCouponTypeParams(CouponCreateReq req) {
        int type = req.getCouponType();
        if (type == Coupon.TYPE_FULL_REDUCTION && req.getFaceValue() == null) {
            throw new BusinessException(MarketingErrorCode.COUPON_TYPE_PARAM_MISSING);
        }
        if (type == Coupon.TYPE_DISCOUNT && req.getDiscountRate() == null) {
            throw new BusinessException(MarketingErrorCode.COUPON_TYPE_PARAM_MISSING);
        }
    }
}
