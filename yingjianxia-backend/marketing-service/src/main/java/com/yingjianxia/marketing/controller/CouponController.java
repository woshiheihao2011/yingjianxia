package com.yingjianxia.marketing.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.marketing.entity.CouponRecord;
import com.yingjianxia.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券 Controller（前端兼容路径 /api/v1/coupons）
 */
@Tag(name = "优惠券服务")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final MarketingService service;

    @Operation(summary = "优惠券列表（全部可领取）")
    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(service.listMyCoupons(UserContext.requiredUserId(), null));
    }

    @Operation(summary = "我的优惠券列表")
    @GetMapping("/mine")
    public ApiResponse<List<CouponRecord>> mine(
            @RequestParam(value = "status", required = false) String status) {
        Integer statusCode = parseStatus(status);
        return ApiResponse.success(service.listMyCoupons(UserContext.requiredUserId(), statusCode));
    }

    /**
     * 将前端传入的状态字符串转换为数据库状态码。
     * 支持 "available"/"unused" → 0, "used" → 1, "expired" → 2，以及数字字符串。
     */
    private Integer parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "available", "unused" -> CouponRecord.STATUS_UNUSED;
            case "used" -> CouponRecord.STATUS_USED;
            case "expired" -> CouponRecord.STATUS_EXPIRED;
            default -> {
                try {
                    yield Integer.parseInt(status.trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/{couponId}/claim")
    public ApiResponse<?> claim(@PathVariable Long couponId) {
        return ApiResponse.success(service.claimCoupon(couponId, UserContext.requiredUserId()));
    }

    @Operation(summary = "可用优惠券（按订单金额过滤）")
    @GetMapping("/available")
    public ApiResponse<?> available(@RequestParam(value = "orderAmount", required = false) java.math.BigDecimal orderAmount) {
        return ApiResponse.success(service.listAvailableCoupons(UserContext.requiredUserId(), orderAmount));
    }
}
