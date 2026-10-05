package com.yingjianxia.marketing.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.marketing.dto.*;
import com.yingjianxia.marketing.entity.Coupon;
import com.yingjianxia.marketing.entity.CouponRecord;
import com.yingjianxia.marketing.entity.Promotion;
import com.yingjianxia.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

/**
 * 营销服务 Controller
 * <p>
 * 分三组端点：
 * <ol>
 *   <li>卖家端：创建券/暂停/结束券、营销活动管理</li>
 *   <li>买家端：领券/我的券/签到/积分</li>
 *   <li>内部端：核销券/退还券/积分扣减/积分奖励（订单服务调用）</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "营销服务", description = "优惠券(创建/领取/核销/退还)、积分(签到/增加/扣减/过期)、营销活动")
@RestController
@RequestMapping({"/api/v1/marketing", "/api/v1/analytics", "/api/v1/promotions"})
@RequiredArgsConstructor
public class MarketingController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final MarketingService service;

    /* ================================================================
     *  卖家端 — 优惠券
     * ================================================================ */

    @Operation(summary = "[卖家] 创建优惠券")
    @PostMapping("/coupons")
    public ApiResponse<Coupon> createCoupon(@Valid @RequestBody CouponCreateReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(service.createCoupon(req, sellerId));
    }

    @Operation(summary = "[卖家] 暂停优惠券")
    @PostMapping("/coupons/{couponId}/pause")
    public ApiResponse<Void> pauseCoupon(@PathVariable Long couponId) {
        Long sellerId = UserContext.requiredUserId();
        service.pauseCoupon(couponId, sellerId);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 结束优惠券")
    @PostMapping("/coupons/{couponId}/end")
    public ApiResponse<Void> endCoupon(@PathVariable Long couponId) {
        Long sellerId = UserContext.requiredUserId();
        service.endCoupon(couponId, sellerId);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 查看店铺优惠券列表")
    @GetMapping("/shops/{shopId}/coupons")
    public ApiResponse<List<Coupon>> listCoupons(@PathVariable Long shopId) {
        return ApiResponse.success(service.listCouponsByShop(shopId));
    }

    /* ================================================================
     *  买家端 — 优惠券
     * ================================================================ */

    @Operation(summary = "[买家] 领取优惠券")
    @PostMapping("/coupons/{couponId}/claim")
    public ApiResponse<CouponClaimResp> claimCoupon(@PathVariable Long couponId) {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(service.claimCoupon(couponId, userId));
    }

    @Operation(summary = "[买家] 查看我的优惠券")
    @GetMapping("/coupons/mine")
    public ApiResponse<List<CouponRecord>> myCoupons(@RequestParam(required = false) Integer status) {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(service.listMyCoupons(userId, status));
    }

    /* ================================================================
     *  买家端 — 积分 & 签到
     * ================================================================ */

    @Operation(summary = "[买家] 每日签到")
    @PostMapping("/points/signin")
    public ApiResponse<SigninResp> signin() {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(service.dailySignin(userId));
    }

    @Operation(summary = "[买家] 查询我的积分")
    @GetMapping("/points")
    public ApiResponse<PointsQueryResp> queryPoints() {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(service.queryPoints(userId));
    }

    /* ================================================================
     *  卖家端 — 营销活动
     * ================================================================ */

    @Operation(summary = "[卖家] 创建营销活动")
    @PostMapping
    public ApiResponse<Promotion> createPromotion(@Valid @RequestBody PromotionCreateReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(service.createPromotion(req, sellerId));
    }

    @Operation(summary = "[卖家] 暂停营销活动")
    @PostMapping("/promotions/{promotionId}/pause")
    public ApiResponse<Void> pausePromotion(@PathVariable Long promotionId) {
        Long sellerId = UserContext.requiredUserId();
        service.pausePromotion(promotionId, sellerId);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 查看店铺营销活动列表")
    @GetMapping("/shops/{shopId}/promotions")
    public ApiResponse<List<Promotion>> listPromotions(@PathVariable Long shopId) {
        return ApiResponse.success(service.listPromotionsByShop(shopId));
    }

    @Operation(summary = "营销活动列表")
    @GetMapping({"", "/"})
    public ApiResponse<List<Promotion>> allPromotions() {
        return ApiResponse.success(service.listAllPromotions());
    }

    /* ================================================================
     *  [内部] 订单服务回调
     * ================================================================ */

    @Operation(summary = "[内部] 核销优惠券（下单时）", hidden = true)
    @PostMapping("/internal/coupons/use")
    public ApiResponse<Void> useCoupon(@Valid @RequestBody CouponUseReq req,
                                        @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        Long userId = UserContext.currentUserId() != null ? UserContext.currentUserId() : 0L;
        service.useCoupon(req, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] 退还优惠券（取消订单时）", hidden = true)
    @PostMapping("/internal/coupons/{couponRecordId}/refund")
    public ApiResponse<Void> refundCoupon(@PathVariable Long couponRecordId,
                                           @RequestParam Long orderId,
                                           @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.refundCoupon(couponRecordId, orderId);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] 积分扣减（下单抵扣）", hidden = true)
    @PostMapping("/internal/points/deduct")
    public ApiResponse<Void> deductPoints(@RequestParam Long userId,
                                           @RequestParam Integer amount,
                                           @RequestParam Long orderId,
                                           @RequestParam(required = false) String remark,
                                           @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.deductPoints(userId, amount, orderId, remark);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] 积分奖励（订单完成奖励）", hidden = true)
    @PostMapping("/internal/points/reward")
    public ApiResponse<Void> addPoints(@RequestParam Long userId,
                                        @RequestParam Integer amount,
                                        @RequestParam String source,
                                        @RequestParam(required = false) Long relatedId,
                                        @RequestParam(required = false) String remark,
                                        @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.addPoints(userId, amount, source, relatedId, remark);
        return ApiResponse.success();
    }

    /* ================================================================
     *  卖家端 — 数据统计
     * ================================================================ */

    @Operation(summary = "[卖家] 经营数据统计")
    @GetMapping({"/analytics/seller", "/seller"})
    public ApiResponse<Map<String, Object>> sellerAnalytics() {
        // TODO: 接入 order-service / product-service 真实统计数据
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalSales", 0.0);
        data.put("orderCount", 0);
        data.put("visitorCount", 0);
        data.put("conversionRate", 0.0);
        data.put("productCount", 0);
        return ApiResponse.success(data);
    }
}
