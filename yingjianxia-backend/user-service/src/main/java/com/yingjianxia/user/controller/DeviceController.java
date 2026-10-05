package com.yingjianxia.user.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.utils.JwtUtils;
import com.yingjianxia.user.dto.auth.LoginDeviceResp;
import com.yingjianxia.user.service.AuthService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录设备管理 Controller — 个人中心 → 账号安全
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Tag(name = "登录设备管理", description = "查询登录设备、踢出指定设备、退出所有其他设备")
@RestController
@RequestMapping("/api/v1/user/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "获取当前用户所有登录设备列表")
    @GetMapping
    public ApiResponse<List<LoginDeviceResp>> listDevices(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = UserContext.requiredUserId();
        String currentJti = extractJti(authHeader);
        return ApiResponse.success(authService.listDevices(userId, currentJti));
    }

    @Operation(summary = "踢出指定设备", description = "将该设备的 Access Token 加入黑名单")
    @DeleteMapping("/{jti}")
    public ApiResponse<Void> kickDevice(@PathVariable("jti") String jti) {
        Long userId = UserContext.requiredUserId();
        authService.kickDevice(userId, jti);
        return ApiResponse.success();
    }

    @Operation(summary = "退出所有其他设备", description = "保留当前 Token，踢出其余所有设备")
    @DeleteMapping
    public ApiResponse<Void> kickAllOtherDevices(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = UserContext.requiredUserId();
        String currentJti = extractJti(authHeader);
        authService.kickAllOtherDevices(userId, currentJti);
        return ApiResponse.success();
    }

    /* ======================== 工具 ======================== */

    /**
     * 从 Authorization 头中提取当前 Access Token 的 JTI
     */
    private String extractJti(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return null;
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        try {
            Claims claims = jwtUtils.parseToken(token);
            return claims.getId();
        } catch (Exception e) {
            log.warn("【设备管理】解析当前 Token JTI 失败: {}", e.getMessage());
            return null;
        }
    }
}
