package com.yingjianxia.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ResultCode;
import com.yingjianxia.user.constants.UserConstants;
import com.yingjianxia.user.dto.shop.*;
import com.yingjianxia.user.entity.*;
import com.yingjianxia.user.enums.UserErrorCode;
import com.yingjianxia.user.feign.EvaluationFeignClient;
import com.yingjianxia.user.mapper.*;
import com.yingjianxia.user.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 店铺服务实现
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final ShopSettingsMapper shopSettingsMapper;
    private final UserMapper userMapper;
    private final UserRealNameMapper realNameMapper;
    private final AddressMapper addressMapper;
    private final ShopLogisticsTemplateMapper logisticsMapper;
    private final ShopServicePromiseMapper promiseMapper;
    private final ShopStaffMapper staffMapper;
    private final ShopCertificationMapper certificationMapper;
    private final ShopFinanceMapper financeMapper;
    private final ObjectMapper objectMapper;
    private final EvaluationFeignClient evaluationFeignClient;

    @Value("${yingjianxia.upload.shop-dir:./uploads/shop}")
    private String shopUploadDir;

    @Value("${yingjianxia.upload.url-prefix:/uploads/shop}")
    private String shopUploadUrlPrefix;

    /* ============================================================
     *  一键开店
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long openShop(Long sellerId, OpenShopReq req) {
        // 1. 一人一店
        Long has = shopMapper.selectCount(new LambdaQueryWrapper<Shop>().eq(Shop::getSellerId, sellerId));
        if (has != null && has > 0) {
            throw new BusinessException(UserErrorCode.SHOP_ALREADY_EXISTS);
        }

        // 2. 名称重复性（允许名称相同？PRD未硬性全局唯一，但做"弱唯一+提示"；当前允许重名，仅长度校验）
        if (req.getShopName().length() > 100) {
            throw new BusinessException(UserErrorCode.SHOP_NAME_TOO_LONG);
        }

        // 3. 创建店铺
        Shop shop = new Shop();
        shop.setSellerId(sellerId);
        shop.setShopName(req.getShopName());
        shop.setDescription(req.getDescription());
        shop.setLogoUrl(req.getLogoUrl());
        shop.setRating(UserConstants.DEFAULT_SHOP_RATING);
        shop.setFollowerCount(0);
        shop.setSaleCount(0);
        shop.setOnSaleCount(0);
        shop.setVerified(false);
        shopMapper.insert(shop);

        // 4. 创建默认店铺设置
        ShopSettings settings = new ShopSettings();
        settings.setShopId(shop.getId());
        settings.setAcceptBargain(true);
        settings.setSupportFaceTrade(false);
        settings.setShipTimePromise(48);
        shopSettingsMapper.insert(settings);

        log.info("【一键开店】卖家={}, 店铺={}, 店铺ID={}", sellerId, shop.getShopName(), shop.getId());
        return shop.getId();
    }

    /* ============================================================
     *  修改店铺基础信息
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShop(Long userId, Long shopId, UpdateShopReq req) {
        if (req == null) return;
        Shop shop = assertShopOwner(userId, shopId);

        LambdaUpdateWrapper<Shop> uw = new LambdaUpdateWrapper<>();
        uw.eq(Shop::getId, shopId);
        boolean need = false;

        if (StrUtil.isNotBlank(req.getShopName())) {
            if (req.getShopName().length() > 100) throw new BusinessException(UserErrorCode.SHOP_NAME_TOO_LONG);
            uw.set(Shop::getShopName, req.getShopName());
            need = true;
        }
        if (req.getDescription() != null) {
            if (req.getDescription().length() > 1000) {
                throw new BusinessException("店铺简介过长");
            }
            uw.set(Shop::getDescription, req.getDescription());
            need = true;
        }
        if (req.getLogoUrl() != null) {
            uw.set(Shop::getLogoUrl, req.getLogoUrl());
            need = true;
        }
        if (req.getCoverUrl() != null) {
            uw.set(Shop::getCoverUrl, req.getCoverUrl());
            need = true;
        }
        if (need) {
            uw.set(Shop::getUpdatedAt, LocalDateTime.now());
            shopMapper.update(null, uw);
        }
    }

    /* ============================================================
     *  修改店铺设置
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShopSettings(Long userId, Long shopId, ShopSettingsReq req) {
        if (req == null) return;
        Shop shop = assertShopOwner(userId, shopId);

        // 校验退货地址是否属于当前用户
        if (req.getReturnAddressId() != null) {
            Address addr = addressMapper.selectById(req.getReturnAddressId());
            if (addr == null || !userId.equals(addr.getUserId())) {
                throw new BusinessException("退货地址不存在或不属于当前用户");
            }
        }

        ShopSettings settings = shopSettingsMapper.selectOne(
                new LambdaQueryWrapper<ShopSettings>().eq(ShopSettings::getShopId, shopId));
        if (settings == null) {
            // 兜底创建（历史数据兼容）
            settings = new ShopSettings();
            settings.setShopId(shopId);
            settings.setAcceptBargain(true);
            settings.setSupportFaceTrade(false);
            settings.setShipTimePromise(48);
            applySettings(settings, req);
            shopSettingsMapper.insert(settings);
            return;
        }

        applySettings(settings, req);
        settings.setUpdatedAt(LocalDateTime.now());
        shopSettingsMapper.updateById(settings);
    }

    private void applySettings(ShopSettings target, ShopSettingsReq req) {
        if (req.getContactPhone() != null) target.setContactPhone(req.getContactPhone());
        if (req.getContactWechat() != null) target.setContactWechat(req.getContactWechat());
        if (req.getBusinessHours() != null) target.setBusinessHours(req.getBusinessHours());
        if (req.getReturnAddressId() != null) target.setReturnAddressId(req.getReturnAddressId());
        if (req.getDefaultExpress() != null) target.setDefaultExpress(req.getDefaultExpress());
        if (req.getAcceptBargain() != null) target.setAcceptBargain(req.getAcceptBargain());
        if (req.getSupportFaceTrade() != null) target.setSupportFaceTrade(req.getSupportFaceTrade());
        if (req.getShipTimePromise() != null) target.setShipTimePromise(req.getShipTimePromise());
        if (req.getAnnouncement() != null) target.setAnnouncement(req.getAnnouncement());
    }

    /* ============================================================
     *  店铺详情查询
     * ============================================================ */

    @Override
    public ShopResp getShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) throw new BusinessException(UserErrorCode.SHOP_NOT_FOUND);
        return buildShopResp(shop);
    }

    @Override
    public ShopResp getShopBySeller(Long sellerId) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getSellerId, sellerId));
        if (shop == null) return null;
        return buildShopResp(shop);
    }

    /* ============================================================
     *  优质卖家认证
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applySellerVerification(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);

        // 1. 已认证不可重复
        if (Boolean.TRUE.equals(user.getSellerVerified())) {
            throw new BusinessException(UserErrorCode.SELLER_VERIFY_ALREADY);
        }

        // 2. 已实名认证
        UserRealName rn = realNameMapper.selectOne(
                new LambdaQueryWrapper<UserRealName>().eq(UserRealName::getUserId, userId));
        if (rn == null || rn.getVerificationStatus() != UserRealName.STATUS_PASSED) {
            throw new BusinessException(UserErrorCode.SELLER_VERIFY_NO_REALNAME);
        }

        // 3. 已开店
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getSellerId, userId));
        if (shop == null) {
            throw new BusinessException(UserErrorCode.SELLER_VERIFY_NO_SHOP);
        }

        // 4. 成交笔数 >= 10
        int tx = user.getTransactionCount() == null ? 0 : user.getTransactionCount();
        if (tx < UserConstants.SELLER_VERIFY_TX_THRESHOLD) {
            throw new BusinessException(UserErrorCode.SELLER_VERIFY_INSUFFICIENT_TX);
        }

        // 5. 信用分 >= 4.5
        BigDecimal credit = user.getCreditScore() == null ? BigDecimal.ZERO : user.getCreditScore();
        if (credit.compareTo(UserConstants.SELLER_VERIFY_CREDIT_THRESHOLD) < 0) {
            throw new BusinessException(UserErrorCode.SELLER_VERIFY_LOW_CREDIT);
        }

        // 6. 打标：用户 seller_verified + 店铺 verified
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getSellerVerified, true)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shop.getId())
                .set(Shop::getVerified, true)
                .set(Shop::getUpdatedAt, LocalDateTime.now()));

        log.info("【优质卖家认证通过】userId={}, shopId={}, tx={}, credit={}", userId, shop.getId(), tx, credit);
    }

    /* ============================================================
     *  事件驱动：指标刷新（MQ 消费：订单完成/评价发布/商品发布下架...）
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshShopMetrics(Long shopId, Long sellerId) {
        // 成交量：users.transaction_count（用户域）≈ shop.sale_count（店铺域冗余）
        User user = userMapper.selectById(sellerId);
        if (user == null) return;
        int txCount = user.getTransactionCount() == null ? 0 : user.getTransactionCount();

        // 评分计算：店铺评分冗余字段，按评价域的平均评分同步（此处占位：取 users.creditScore * 权重 或 直接保持原值）
        // PRD 约定 rating 来自评价域 aggregate，这里只更新 sale_count / on_sale_count 冗余（on_sale_count 由商品域 MQ 更新）
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .set(Shop::getSaleCount, txCount)
                .set(Shop::getUpdatedAt, LocalDateTime.now()));

        log.info("【店铺指标刷新】shopId={}, saleCount={}", shopId, txCount);
    }

    /* ============================================================
     *  店铺设置查询（GET）
     * ============================================================ */

    @Override
    public ShopSettingsResp getShopSettings(Long sellerId) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getSellerId, sellerId));
        if (shop == null) throw new BusinessException(UserErrorCode.SHOP_NOT_FOUND);

        ShopSettingsResp resp = new ShopSettingsResp();
        ShopSettings s = shopSettingsMapper.selectOne(
                new LambdaQueryWrapper<ShopSettings>().eq(ShopSettings::getShopId, shop.getId()));
        if (s == null) {
            resp.setAcceptBargain(true);
            resp.setSupportFaceTrade(false);
            resp.setShipTimePromise(48);
        } else {
            resp.setContactPhone(s.getContactPhone());
            resp.setContactWechat(s.getContactWechat());
            resp.setBusinessHours(s.getBusinessHours());
            resp.setReturnAddressId(s.getReturnAddressId());
            resp.setDefaultExpress(s.getDefaultExpress());
            resp.setAcceptBargain(s.getAcceptBargain());
            resp.setSupportFaceTrade(s.getSupportFaceTrade());
            resp.setShipTimePromise(s.getShipTimePromise());
            resp.setAnnouncement(s.getAnnouncement());
        }

        // 默认运费模板
        ShopLogisticsTemplate defaultTpl = logisticsMapper.selectOne(
                new LambdaQueryWrapper<ShopLogisticsTemplate>()
                        .eq(ShopLogisticsTemplate::getShopId, shop.getId())
                        .eq(ShopLogisticsTemplate::getIsDefault, true)
                        .last("LIMIT 1"));
        if (defaultTpl != null) {
            resp.setLogisticsId(defaultTpl.getId());
        }

        // 服务承诺
        resp.setServicePromises(listServicePromises(sellerId));

        return resp;
    }

    /* ============================================================
     *  运费模板管理
     * ============================================================ */

    @Override
    public List<LogisticsTemplateResp> listLogisticsTemplates(Long sellerId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        List<ShopLogisticsTemplate> list = logisticsMapper.selectList(
                new LambdaQueryWrapper<ShopLogisticsTemplate>().eq(ShopLogisticsTemplate::getShopId, shop.getId())
                        .orderByDesc(ShopLogisticsTemplate::getIsDefault)
                        .orderByAsc(ShopLogisticsTemplate::getCreatedAt));
        List<LogisticsTemplateResp> result = new ArrayList<>();
        for (ShopLogisticsTemplate t : list) {
            result.add(toTemplateResp(t));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLogisticsTemplate(Long sellerId, LogisticsTemplateReq req) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        // 若设为默认，先取消其他默认
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            logisticsMapper.update(null, new LambdaUpdateWrapper<ShopLogisticsTemplate>()
                    .eq(ShopLogisticsTemplate::getShopId, shop.getId())
                    .set(ShopLogisticsTemplate::getIsDefault, false));
        }
        ShopLogisticsTemplate t = new ShopLogisticsTemplate();
        t.setShopId(shop.getId());
        t.setName(req.getName());
        t.setType(req.getType());
        t.setBaseFee(req.getBaseFee());
        t.setBaseUnit(req.getBaseUnit());
        t.setStepFee(req.getStepFee());
        t.setStepUnit(req.getStepUnit());
        t.setFreeShippingThreshold(req.getFreeShippingThreshold());
        t.setIsDefault(req.getIsDefault());
        t.setRegion(req.getRegion());
        logisticsMapper.insert(t);
        log.info("【创建运费模板】shopId={}, templateId={}, name={}", shop.getId(), t.getId(), t.getName());
        return t.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLogisticsTemplate(Long sellerId, Long templateId, LogisticsTemplateReq req) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        ShopLogisticsTemplate t = logisticsMapper.selectOne(
                new LambdaQueryWrapper<ShopLogisticsTemplate>()
                        .eq(ShopLogisticsTemplate::getId, templateId)
                        .eq(ShopLogisticsTemplate::getShopId, shop.getId()));
        if (t == null) throw new BusinessException(ResultCode.DATA_NOT_FOUND);

        if (Boolean.TRUE.equals(req.getIsDefault()) && !Boolean.TRUE.equals(t.getIsDefault())) {
            logisticsMapper.update(null, new LambdaUpdateWrapper<ShopLogisticsTemplate>()
                    .eq(ShopLogisticsTemplate::getShopId, shop.getId())
                    .set(ShopLogisticsTemplate::getIsDefault, false));
        }
        if (req.getName() != null) t.setName(req.getName());
        if (req.getType() != null) t.setType(req.getType());
        if (req.getBaseFee() != null) t.setBaseFee(req.getBaseFee());
        if (req.getBaseUnit() != null) t.setBaseUnit(req.getBaseUnit());
        if (req.getStepFee() != null) t.setStepFee(req.getStepFee());
        if (req.getStepUnit() != null) t.setStepUnit(req.getStepUnit());
        if (req.getFreeShippingThreshold() != null) t.setFreeShippingThreshold(req.getFreeShippingThreshold());
        if (req.getIsDefault() != null) t.setIsDefault(req.getIsDefault());
        if (req.getRegion() != null) t.setRegion(req.getRegion());
        t.setUpdatedAt(LocalDateTime.now());
        logisticsMapper.updateById(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLogisticsTemplate(Long sellerId, Long templateId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        logisticsMapper.delete(new LambdaQueryWrapper<ShopLogisticsTemplate>()
                .eq(ShopLogisticsTemplate::getId, templateId)
                .eq(ShopLogisticsTemplate::getShopId, shop.getId()));
    }

    private LogisticsTemplateResp toTemplateResp(ShopLogisticsTemplate t) {
        LogisticsTemplateResp r = new LogisticsTemplateResp();
        r.setId(t.getId());
        r.setShopId(t.getShopId());
        r.setName(t.getName());
        r.setType(t.getType());
        r.setBaseFee(t.getBaseFee());
        r.setBaseUnit(t.getBaseUnit());
        r.setStepFee(t.getStepFee());
        r.setStepUnit(t.getStepUnit());
        r.setFreeShippingThreshold(t.getFreeShippingThreshold());
        r.setIsDefault(t.getIsDefault());
        r.setRegion(t.getRegion());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }

    /* ============================================================
     *  服务承诺管理
     * ============================================================ */

    private static final Map<String, String> DEFAULT_PROMISES = new LinkedHashMap<>();
    static {
        DEFAULT_PROMISES.put("7day_return", "7天无理由退货");
        DEFAULT_PROMISES.put("fake_one_pay_three", "假一赔三");
        DEFAULT_PROMISES.put("fast_refund", "极速退款");
        DEFAULT_PROMISES.put("shipping_insurance", "运费险");
        DEFAULT_PROMISES.put("quality_guarantee", "品质保障");
    }

    @Override
    public List<ServicePromiseResp> listServicePromises(Long sellerId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        List<ShopServicePromise> list = promiseMapper.selectList(
                new LambdaQueryWrapper<ShopServicePromise>().eq(ShopServicePromise::getShopId, shop.getId()));
        Map<String, ShopServicePromise> existing = new HashMap<>();
        for (ShopServicePromise p : list) {
            existing.put(p.getCode(), p);
        }

        // 对缺失的默认承诺，自动落库以便前端可通过 ID 更新开关
        List<ServicePromiseResp> result = new ArrayList<>();
        for (Map.Entry<String, String> e : DEFAULT_PROMISES.entrySet()) {
            ShopServicePromise p = existing.get(e.getKey());
            if (p == null) {
                p = new ShopServicePromise();
                p.setShopId(shop.getId());
                p.setCode(e.getKey());
                p.setName(e.getValue());
                p.setEnabled(false);
                promiseMapper.insert(p);
            }
            ServicePromiseResp r = new ServicePromiseResp();
            r.setId(p.getId());
            r.setCode(p.getCode());
            r.setName(p.getName());
            r.setEnabled(p.getEnabled());
            result.add(r);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServicePromise(Long sellerId, Long promiseId, Boolean enabled) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        ShopServicePromise p = promiseMapper.selectOne(
                new LambdaQueryWrapper<ShopServicePromise>()
                        .eq(ShopServicePromise::getId, promiseId)
                        .eq(ShopServicePromise::getShopId, shop.getId()));
        if (p == null) throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        p.setEnabled(enabled);
        p.setUpdatedAt(LocalDateTime.now());
        promiseMapper.updateById(p);
    }

    /* ============================================================
     *  员工管理
     * ============================================================ */

    @Override
    public List<StaffResp> listStaff(Long sellerId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        List<ShopStaff> list = staffMapper.selectList(
                new LambdaQueryWrapper<ShopStaff>().eq(ShopStaff::getShopId, shop.getId())
                        .orderByAsc(ShopStaff::getCreatedAt));
        List<StaffResp> result = new ArrayList<>();
        for (ShopStaff s : list) {
            result.add(toStaffResp(s));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStaff(Long sellerId, StaffReq req) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        ShopStaff staff = new ShopStaff();
        staff.setShopId(shop.getId());
        staff.setUserId(req.getUserId());
        staff.setRole(req.getRole());
        staff.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        staff.setPermissions(permissionsToJson(req.getPermissions()));
        staffMapper.insert(staff);
        log.info("【添加员工】shopId={}, staffId={}, role={}", shop.getId(), staff.getId(), staff.getRole());
        return staff.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStaff(Long sellerId, Long staffId, StaffReq req) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        ShopStaff staff = staffMapper.selectOne(
                new LambdaQueryWrapper<ShopStaff>()
                        .eq(ShopStaff::getId, staffId)
                        .eq(ShopStaff::getShopId, shop.getId()));
        if (staff == null) throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        if (req.getUserId() != null) staff.setUserId(req.getUserId());
        if (req.getRole() != null) staff.setRole(req.getRole());
        if (req.getStatus() != null) staff.setStatus(req.getStatus());
        if (req.getPermissions() != null) staff.setPermissions(permissionsToJson(req.getPermissions()));
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStaff(Long sellerId, Long staffId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        staffMapper.delete(new LambdaQueryWrapper<ShopStaff>()
                .eq(ShopStaff::getId, staffId)
                .eq(ShopStaff::getShopId, shop.getId()));
    }

    private StaffResp toStaffResp(ShopStaff s) {
        StaffResp r = new StaffResp();
        r.setId(s.getId());
        r.setShopId(s.getShopId());
        r.setUserId(s.getUserId());
        r.setRole(s.getRole());
        r.setStatus(s.getStatus());
        r.setCreatedAt(s.getCreatedAt());
        r.setPermissions(permissionsFromJson(s.getPermissions()));
        if (s.getUserId() != null) {
            User u = userMapper.selectById(s.getUserId());
            if (u != null) r.setNickname(u.getNickname());
        }
        return r;
    }

    private String permissionsToJson(List<String> permissions) {
        if (permissions == null) return null;
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> permissionsFromJson(String json) {
        if (StrUtil.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /* ============================================================
     *  认证资质
     * ============================================================ */

    @Override
    public List<CertificationResp> listCertifications(Long sellerId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        List<ShopCertification> list = certificationMapper.selectList(
                new LambdaQueryWrapper<ShopCertification>().eq(ShopCertification::getShopId, shop.getId()));

        // 若店铺无认证记录，根据现有数据生成默认列表
        if (list == null || list.isEmpty()) {
            return buildDefaultCertifications(shop);
        }

        List<CertificationResp> result = new ArrayList<>();
        for (ShopCertification c : list) {
            result.add(toCertResp(c));
        }
        return result;
    }

    private List<CertificationResp> buildDefaultCertifications(Shop shop) {
        List<CertificationResp> result = new ArrayList<>();
        // 优质卖家认证（关联 shop.verified）
        CertificationResp seller = new CertificationResp();
        seller.setType("seller_verified");
        seller.setName("优质卖家认证");
        seller.setStatus(Boolean.TRUE.equals(shop.getVerified()) ? "approved" : "not_applied");
        if (Boolean.TRUE.equals(shop.getVerified())) {
            seller.setCertifiedAt(shop.getUpdatedAt());
        }
        result.add(seller);

        // 营业执照（默认未申请）
        CertificationResp license = new CertificationResp();
        license.setType("business_license");
        license.setName("营业执照");
        license.setStatus("not_applied");
        result.add(license);

        // 保证金（默认未缴纳）
        CertificationResp deposit = new CertificationResp();
        deposit.setType("deposit");
        deposit.setName("保证金");
        deposit.setStatus("not_applied");
        result.add(deposit);

        // 类目准入（默认未申请）
        CertificationResp category = new CertificationResp();
        category.setType("category_access");
        category.setName("类目准入");
        category.setStatus("not_applied");
        result.add(category);
        return result;
    }

    private CertificationResp toCertResp(ShopCertification c) {
        CertificationResp r = new CertificationResp();
        r.setId(c.getId());
        r.setType(c.getType());
        r.setName(c.getName());
        r.setStatus(c.getStatus());
        r.setCertifiedAt(c.getCertifiedAt());
        r.setExpireAt(c.getExpireAt());
        return r;
    }

    /* ============================================================
     *  财务设置
     * ============================================================ */

    @Override
    public FinanceResp getFinance(Long sellerId) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        ShopFinance f = financeMapper.selectOne(
                new LambdaQueryWrapper<ShopFinance>().eq(ShopFinance::getShopId, shop.getId()));
        FinanceResp r = new FinanceResp();
        r.setShopId(shop.getId());
        if (f == null) {
            r.setWithdrawThreshold(BigDecimal.ZERO);
            r.setWithdrawFeeRate(BigDecimal.ZERO);
            r.setSettlementCycle("weekly");
            return r;
        }
        r.setBankAccountName(f.getBankAccountName());
        r.setBankAccountNo(maskBankNo(f.getBankAccountNo()));
        r.setBankName(f.getBankName());
        r.setWithdrawThreshold(f.getWithdrawThreshold());
        r.setWithdrawFeeRate(f.getWithdrawFeeRate());
        r.setSettlementCycle(f.getSettlementCycle());
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinance(Long sellerId, FinanceReq req) {
        Shop shop = getShopBySellerOrThrow(sellerId);
        ShopFinance f = financeMapper.selectOne(
                new LambdaQueryWrapper<ShopFinance>().eq(ShopFinance::getShopId, shop.getId()));
        if (f == null) {
            f = new ShopFinance();
            f.setShopId(shop.getId());
            f.setWithdrawThreshold(BigDecimal.ZERO);
            f.setWithdrawFeeRate(BigDecimal.ZERO);
            f.setSettlementCycle("weekly");
            applyFinance(f, req);
            financeMapper.insert(f);
            return;
        }
        applyFinance(f, req);
        f.setUpdatedAt(LocalDateTime.now());
        financeMapper.updateById(f);
    }

    private void applyFinance(ShopFinance f, FinanceReq req) {
        if (req.getBankAccountName() != null) f.setBankAccountName(req.getBankAccountName());
        if (req.getBankAccountNo() != null) f.setBankAccountNo(req.getBankAccountNo());
        if (req.getBankName() != null) f.setBankName(req.getBankName());
        if (req.getWithdrawThreshold() != null) f.setWithdrawThreshold(req.getWithdrawThreshold());
        if (req.getWithdrawFeeRate() != null) f.setWithdrawFeeRate(req.getWithdrawFeeRate());
        if (req.getSettlementCycle() != null) f.setSettlementCycle(req.getSettlementCycle());
    }

    private String maskBankNo(String no) {
        if (StrUtil.isBlank(no) || no.length() < 8) return no;
        return no.substring(0, 4) + "****" + no.substring(no.length() - 4);
    }

    /* ============================================================
     *  店铺图片上传
     * ============================================================ */

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Override
    public String uploadShopImage(Long sellerId, MultipartFile file, String type) {
        // 1. 校验店铺存在
        getShopBySellerOrThrow(sellerId);

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_EMPTY);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_TYPE_NOT_ALLOWED);
        }

        // 3. 校验类型
        if (!"logo".equalsIgnoreCase(type) && !"banner".equalsIgnoreCase(type)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "type 只能是 logo 或 banner");
        }

        // 4. 生成文件名：{type}_{sellerId}_{timestamp}_{random}.{ext}
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = type.toLowerCase() + "_" + sellerId + "_" + System.currentTimeMillis()
                + "_" + new Random().nextInt(10000) + ext;

        // 5. 保存文件（使用绝对路径，避免相对路径被 Tomcat 临时目录解析）
        File dir = new File(shopUploadDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("[店铺上传] 创建目录失败: {}", dir.getAbsolutePath());
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "上传目录创建失败");
        }
        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("[店铺上传] 保存文件失败: {}", filename, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件保存失败");
        }

        // 6. 返回可访问 URL（相对路径，前端通过网关访问）
        return shopUploadUrlPrefix + "/" + filename;
    }

    /* ============================================================
     *  内部工具
     * ============================================================ */

    /**
     * 获取卖家所属店铺，不存在抛异常
     */
    private Shop getShopBySellerOrThrow(Long sellerId) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getSellerId, sellerId));
        if (shop == null) throw new BusinessException(UserErrorCode.SHOP_NOT_FOUND);
        return shop;
    }

    private Shop assertShopOwner(Long userId, Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) throw new BusinessException(UserErrorCode.SHOP_NOT_FOUND);
        if (!shop.getSellerId().equals(userId)) {
            throw new BusinessException(UserErrorCode.SHOP_NOT_BELONG_TO_USER);
        }
        return shop;
    }

    private ShopResp buildShopResp(Shop shop) {
        ShopResp r = new ShopResp();
        r.setId(shop.getId());
        r.setSellerId(shop.getSellerId());
        r.setShopName(shop.getShopName());
        r.setDescription(shop.getDescription());
        r.setLogoUrl(shop.getLogoUrl());
        r.setCoverUrl(shop.getCoverUrl());
        // Banner 图与封面图同源（bug-20260908130123 前端期望 bannerUrl 字段）
        r.setBannerUrl(shop.getCoverUrl());
        r.setRating(shop.getRating());
        r.setFollowerCount(shop.getFollowerCount());
        r.setSaleCount(shop.getSaleCount());
        // 销量与成交量同义
        r.setSoldCount(shop.getSaleCount());
        r.setOnSaleCount(shop.getOnSaleCount());
        r.setVerified(shop.getVerified());
        r.setCreatedAt(shop.getCreatedAt());

        // 卖家信息：信用分 + 地区
        User seller = userMapper.selectById(shop.getSellerId());
        if (seller != null) {
            r.setSellerCreditScore(seller.getCreditScore());
            r.setRegion(seller.getRegion());
        }

        // 店铺等级：优质卖家=金牌店铺，否则=普通店铺
        r.setShopLevel(Boolean.TRUE.equals(shop.getVerified()) ? "金牌店铺" : "普通店铺");

        // 入驻时间（yyyy-MM-dd 字符串）
        if (shop.getCreatedAt() != null) {
            r.setOpenedAt(shop.getCreatedAt().toLocalDate().toString());
        }

        // 月成交量：暂以累计成交量近似（未单独统计月度数据）
        r.setMonthlySales(shop.getSaleCount());

        // 评价统计：通过 Feign 调用 evaluation-service 获取真实数据（bug-20260908201428）
        Integer reviewCount = null;
        Double avgRating = null;
        Double positiveRate = null;
        try {
            var evalResp = evaluationFeignClient.sellerReviewStats(shop.getSellerId());
            if (evalResp != null && evalResp.isSuccess() && evalResp.getData() != null) {
                Map<String, Object> evalStats = evalResp.getData();
                reviewCount = evalStats.get("totalCount") != null
                        ? ((Number) evalStats.get("totalCount")).intValue() : 0;
                avgRating = evalStats.get("averageRating") != null
                        ? ((Number) evalStats.get("averageRating")).doubleValue() : null;
                positiveRate = evalStats.get("positiveRate") != null
                        ? ((Number) evalStats.get("positiveRate")).doubleValue() : null;
            }
        } catch (Exception e) {
            log.warn("[buildShopResp-评价统计查询失败] shopId={}, sellerId={}, err={}",
                    shop.getId(), shop.getSellerId(), e.getMessage());
        }

        // 评价数：来自 evaluation-service 真实统计
        r.setReviewCount(reviewCount != null ? reviewCount : 0);

        // 好评率：来自 evaluation-service（好评数/总评价数 × 100）
        if (positiveRate != null) {
            r.setGoodRate(BigDecimal.valueOf(positiveRate).setScale(1, java.math.RoundingMode.HALF_UP));
        } else if (shop.getRating() != null) {
            // 降级：无评价数据时由店铺评分换算
            r.setGoodRate(shop.getRating()
                    .multiply(BigDecimal.valueOf(20))
                    .setScale(1, java.math.RoundingMode.HALF_UP));
        }

        // 评分细分：评价域 Review 表只有总评分，无 descScore/serviceScore/shipScore 细分
        // 使用 evaluation-service 返回的真实平均评分（优先）或 shops 表 rating（降级）
        BigDecimal scoreBase = avgRating != null
                ? BigDecimal.valueOf(avgRating).setScale(1, java.math.RoundingMode.HALF_UP)
                : shop.getRating();
        if (scoreBase != null) {
            r.setDescScore(scoreBase);
            r.setServiceScore(scoreBase);
            r.setShipScore(scoreBase);
        }

        // 店铺设置冗余
        ShopSettings s = shopSettingsMapper.selectOne(
                new LambdaQueryWrapper<ShopSettings>().eq(ShopSettings::getShopId, shop.getId()));
        if (s != null) {
            r.setContactPhone(s.getContactPhone());
            r.setContactWechat(s.getContactWechat());
            r.setBusinessHours(s.getBusinessHours());
            r.setDefaultExpress(s.getDefaultExpress());
            r.setAcceptBargain(s.getAcceptBargain());
            r.setSupportFaceTrade(s.getSupportFaceTrade());
            r.setShipTimePromise(s.getShipTimePromise());
            r.setAnnouncement(s.getAnnouncement());
        }

        // 响应速度：客服首响时长（分钟），发货时效不等于响应速度（bug-20260908201428 修复）
        // 客服域未接入前使用合理默认值 30 分钟
        r.setResponseTime(30);

        // 认证标签：从 ShopCertification 已通过的认证 + 服务承诺组合
        r.setCertifications(buildCertificationTags(shop));

        return r;
    }

    /**
     * 构建店铺认证标签列表（用于店铺主页展示）：
     *  - 已通过的认证项名称
     *  - 优质卖家认证
     *  - 已启用的服务承诺项
     */
    private List<String> buildCertificationTags(Shop shop) {
        List<String> tags = new ArrayList<>();
        // 实名认证（取用户域 UserRealName 状态）
        UserRealName rn = realNameMapper.selectOne(
                new LambdaQueryWrapper<UserRealName>().eq(UserRealName::getUserId, shop.getSellerId()));
        if (rn != null && rn.getVerificationStatus() == UserRealName.STATUS_PASSED) {
            tags.add("实名认证");
        }
        // 优质卖家
        if (Boolean.TRUE.equals(shop.getVerified())) {
            tags.add("金牌卖家");
        }
        // 已通过的店铺认证
        List<ShopCertification> certs = certificationMapper.selectList(
                new LambdaQueryWrapper<ShopCertification>().eq(ShopCertification::getShopId, shop.getId()));
        for (ShopCertification c : certs) {
            if ("approved".equals(c.getStatus()) && !tags.contains(c.getName())) {
                tags.add(c.getName());
            }
        }
        // 已启用的服务承诺（7天无理由、担保交易等）
        List<ShopServicePromise> promises = promiseMapper.selectList(
                new LambdaQueryWrapper<ShopServicePromise>()
                        .eq(ShopServicePromise::getShopId, shop.getId())
                        .eq(ShopServicePromise::getEnabled, true));
        for (ShopServicePromise p : promises) {
            if (!tags.contains(p.getName())) {
                tags.add(p.getName());
            }
        }
        return tags;
    }
}
