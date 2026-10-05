package com.yingjianxia.user.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.user.dto.profile.*;
import com.yingjianxia.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户资料 Controller — 个人中心、实名认证、收货地址
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "用户资料模块", description = "个人中心、实名认证、收货地址")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    /* ======================== 个人中心 ======================== */

    @Operation(summary = "获取个人中心资料")
    @GetMapping("/profile")
    public ApiResponse<UserProfileResp> getProfile() {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(profileService.getProfile(userId));
    }

    @Operation(summary = "修改个人资料（昵称/头像/简介）")
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        Long userId = UserContext.requiredUserId();
        profileService.updateProfile(userId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody java.util.Map<String, String> body) {
        Long userId = UserContext.requiredUserId();
        profileService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return ApiResponse.success();
    }

    /* ======================== 实名认证 ======================== */

    @Operation(summary = "提交实名认证", description = "需先发送 realName 场景的短信验证码")
    @PostMapping("/real-name")
    public ApiResponse<Void> submitRealName(@Valid @RequestBody RealNameSubmitReq req) {
        Long userId = UserContext.requiredUserId();
        profileService.submitRealName(userId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "查询实名状态", description = "0未认证 1已通过 2未通过")
    @GetMapping("/real-name/status")
    public ApiResponse<Integer> getRealNameStatus() {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(profileService.getRealNameStatus(userId));
    }

    /* ======================== 收货地址 ======================== */

    @Operation(summary = "新增收货地址", description = "最多20条，首条默认自动设为默认")
    @PostMapping("/address")
    public ApiResponse<Long> addAddress(@Valid @RequestBody AddressReq req) {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(profileService.addAddress(userId, req));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/address/{id}")
    public ApiResponse<Void> updateAddress(@PathVariable("id") Long addressId,
                                           @Valid @RequestBody AddressReq req) {
        Long userId = UserContext.requiredUserId();
        profileService.updateAddress(userId, addressId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "删除收货地址", description = "默认地址不能删除（若有其他地址）")
    @DeleteMapping("/address/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable("id") Long addressId) {
        Long userId = UserContext.requiredUserId();
        profileService.deleteAddress(userId, addressId);
        return ApiResponse.success();
    }

    @Operation(summary = "设为默认地址")
    @PostMapping("/address/{id}/default")
    public ApiResponse<Void> setDefaultAddress(@PathVariable("id") Long addressId) {
        Long userId = UserContext.requiredUserId();
        profileService.setDefaultAddress(userId, addressId);
        return ApiResponse.success();
    }

    @Operation(summary = "查询地址列表", description = "默认地址排在最前")
    @GetMapping("/addresses")
    public ApiResponse<List<AddressResp>> listAddress() {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(profileService.listAddress(userId));
    }

    @Operation(summary = "查询单个地址详情")
    @GetMapping("/address/{id}")
    public ApiResponse<AddressResp> getAddress(@PathVariable("id") Long addressId) {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(profileService.getAddress(userId, addressId));
    }

    /* ======================== 信用分 ======================== */

    @Operation(summary = "查询当前用户信用分")
    @GetMapping("/credit-score")
    public ApiResponse<java.math.BigDecimal> getCreditScore() {
        Long userId = UserContext.requiredUserId();
        return ApiResponse.success(profileService.getCreditScore(userId));
    }

    /* ======================== [内部] 信用分任务入口（手动触发） ======================== */

    @Operation(summary = "[内部] 手动触发信用分刷新（开发/运维用）", hidden = true)
    @PostMapping("/internal/credit-score/refresh")
    public ApiResponse<Void> refreshCredit() {
        profileService.refreshCreditScoreBatch();
        return ApiResponse.success();
    }
}
