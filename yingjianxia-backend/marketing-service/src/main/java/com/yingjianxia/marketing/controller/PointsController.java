package com.yingjianxia.marketing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.marketing.entity.PointsRecord;
import com.yingjianxia.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 积分 Controller（前端兼容路径 /api/v1/points）
 */
@Tag(name = "积分服务")
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointsController {

    private final MarketingService service;

    @Operation(summary = "积分信息")
    @GetMapping
    public ApiResponse<?> info() {
        return ApiResponse.success(service.queryPoints(UserContext.requiredUserId()));
    }

    @Operation(summary = "每日签到")
    @PostMapping("/sign-in")
    public ApiResponse<?> signIn() {
        return ApiResponse.success(service.dailySignin(UserContext.requiredUserId()));
    }

    @Operation(summary = "积分记录（分页）")
    @GetMapping("/records")
    public ApiResponse<IPage<PointsRecord>> records(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return ApiResponse.success(service.listPointsRecords(UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "积分汇总")
    @GetMapping("/summary")
    public ApiResponse<?> summary() {
        return ApiResponse.success(service.queryPoints(UserContext.requiredUserId()));
    }

    @Operation(summary = "积分兑换优惠券")
    @PostMapping("/redeem")
    public ApiResponse<?> redeem(@RequestBody java.util.Map<String, Object> body) {
        Long couponId = Long.valueOf(String.valueOf(body.get("couponId")));
        return ApiResponse.success(service.redeemPoints(UserContext.requiredUserId(), couponId));
    }
}
