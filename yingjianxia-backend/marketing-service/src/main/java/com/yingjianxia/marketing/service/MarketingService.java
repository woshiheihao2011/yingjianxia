package com.yingjianxia.marketing.service;

import com.yingjianxia.marketing.dto.*;
import com.yingjianxia.marketing.entity.Coupon;
import com.yingjianxia.marketing.entity.CouponRecord;
import com.yingjianxia.marketing.entity.PointsAccount;
import com.yingjianxia.marketing.entity.PointsRecord;
import com.yingjianxia.marketing.entity.Promotion;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 营销服务接口
 * <p>
 * 职责：优惠券CRUD、优惠券领取（限领+原子扣减）、核销、退还；
 * 积分账户初始化、积分增加/扣减/过期处理；营销活动管理。
 *
 * @author 硬件侠后端团队
 */
public interface MarketingService {

    /* ========== 优惠券 — 卖家端 ========== */

    /** 卖家创建优惠券 */
    Coupon createCoupon(CouponCreateReq req, Long creatorId);

    /** 卖家暂停优惠券 */
    void pauseCoupon(Long couponId, Long sellerId);

    /** 卖家结束优惠券 */
    void endCoupon(Long couponId, Long sellerId);

    /** 查询店铺下优惠券列表 */
    List<Coupon> listCouponsByShop(Long shopId);

    /* ========== 优惠券 — 买家端 ========== */

    /** 领取优惠券（限领校验 + 库存原子扣减） */
    CouponClaimResp claimCoupon(Long couponId, Long userId);

    /** 查询我的优惠券 */
    List<CouponRecord> listMyCoupons(Long userId, Integer status);

    /** 查询可用优惠券（按订单金额过滤门槛） */
    List<CouponRecord> listAvailableCoupons(Long userId, java.math.BigDecimal orderAmount);

    /* ========== 优惠券 — 内部调用（订单服务） ========== */

    /** 核销优惠券（下单时校验有效期/门槛 → 标记已使用） */
    void useCoupon(CouponUseReq req, Long userId);

    /** 退还优惠券（取消订单时退还） */
    void refundCoupon(Long couponRecordId, Long orderId);

    /* ========== 积分 — 账户管理 ========== */

    /** 积分账户初始化（首次交互时自动建账户） */
    PointsAccount initPointsAccount(Long userId);

    /** 查询积分 */
    PointsQueryResp queryPoints(Long userId);

    /** 分页查询积分变动记录 */
    IPage<PointsRecord> listPointsRecords(Long userId, int pageNum, int pageSize);

    /* ========== 积分 — 增加与扣减 ========== */

    /** 积分增加（签到/浏览/分享/订单奖励） */
    void addPoints(Long userId, int amount, String source, Long relatedId, String remark);

    /** 积分扣减（下单抵扣） */
    void deductPoints(Long userId, int amount, Long orderId, String remark);

    /** 每日签到 */
    SigninResp dailySignin(Long userId);

    /** 积分兑换优惠券 */
    Object redeemPoints(Long userId, Long couponId);

    /** 积分过期处理（定时任务） */
    int expirePoints();

    /* ========== 营销活动 — 卖家端 ========== */

    /** 创建营销活动 */
    Promotion createPromotion(PromotionCreateReq req, Long sellerId);

    /** 暂停营销活动 */
    void pausePromotion(Long promotionId, Long sellerId);

    /** 查询店铺活动列表 */
    List<Promotion> listPromotionsByShop(Long shopId);

    /** 查询全部活动列表 */
    List<Promotion> listAllPromotions();
}
