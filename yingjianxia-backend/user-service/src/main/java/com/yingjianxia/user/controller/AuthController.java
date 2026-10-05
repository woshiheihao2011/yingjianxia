package com.yingjianxia.user.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.user.dto.auth.*;
import com.yingjianxia.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

/**
 * 认证模块 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "认证模块", description = "短信验证码、注册、登录、Token 刷新、登出、第三方登录")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final AuthService authService;

    @Operation(summary = "发送短信验证码", description = "场景: register(注册)/login(登录)/resetPwd(找回密码)/realName(实名认证)")
    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeReq req) {
        authService.sendSmsCode(req);
        return ApiResponse.success();
    }

    @Operation(summary = "手机号注册", description = "短信验证码 + 密码注册，注册成功直接返回登录态")
    @PostMapping("/register/phone")
    public ApiResponse<LoginResp> phoneRegister(@Valid @RequestBody PhoneRegisterReq req,
                                                HttpServletRequest request) {
        LoginResp resp = authService.phoneRegister(req);
        authService.recordDeviceSession(resp.getUserId(), resp.getAccessToken(), extractDeviceInfo(request));
        return ApiResponse.success(resp);
    }

    @Operation(summary = "密码登录", description = "手机号 + 密码；连续错误 5 次锁定 30 分钟")
    @PostMapping("/login/password")
    public ApiResponse<LoginResp> passwordLogin(@Valid @RequestBody PasswordLoginReq req,
                                                HttpServletRequest request) {
        LoginResp resp = authService.passwordLogin(req);
        authService.recordDeviceSession(resp.getUserId(), resp.getAccessToken(), extractDeviceInfo(request));
        return ApiResponse.success(resp);
    }

    @Operation(summary = "验证码登录", description = "手机号 + 短信验证码；未注册手机号自动注册账号")
    @PostMapping("/login/sms")
    public ApiResponse<LoginResp> smsCodeLogin(@Valid @RequestBody SmsCodeLoginReq req,
                                               HttpServletRequest request) {
        LoginResp resp = authService.smsCodeLogin(req);
        authService.recordDeviceSession(resp.getUserId(), resp.getAccessToken(), extractDeviceInfo(request));
        return ApiResponse.success(resp);
    }

    @Operation(summary = "第三方 OAuth2 登录", description = "支持 weixin/qq/alipay，首次授权自动建号并绑定")
    @PostMapping("/login/oauth")
    public ApiResponse<LoginResp> oauthLogin(@Valid @RequestBody OAuthLoginReq req,
                                             HttpServletRequest request) {
        LoginResp resp = authService.oauthLogin(req);
        authService.recordDeviceSession(resp.getUserId(), resp.getAccessToken(), extractDeviceInfo(request));
        return ApiResponse.success(resp);
    }

    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 换发新的双 Token（滚动续签）")
    @PostMapping("/token/refresh")
    public ApiResponse<LoginResp> refreshToken(@Valid @RequestBody RefreshTokenReq req,
                                               HttpServletRequest request) {
        LoginResp resp = authService.refreshToken(req);
        authService.recordDeviceSession(resp.getUserId(), resp.getAccessToken(), extractDeviceInfo(request));
        return ApiResponse.success(resp);
    }

    @Operation(summary = "重置密码", description = "通过短信验证码重置密码")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody java.util.Map<String, String> body) {
        authService.resetPassword(body.get("phone"), body.get("smsCode"), body.get("newPassword"));
        return ApiResponse.success();
    }

    @Operation(summary = "登出", description = "将当前 Access Token 和 Refresh Token 加入黑名单")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @RequestBody(required = false) RefreshTokenReq refreshReq) {
        String access = extractBearer(authHeader);
        String refresh = refreshReq != null ? refreshReq.getRefreshToken() : null;
        authService.logout(access, refresh);
        return ApiResponse.success();
    }

    /* ======================== 内部（Feign/网关用）API ======================== */

    /**
     * 根据 JWT 的 jti 校验是否在黑名单（网关过滤器通过 Feign 调用）
     */
    @Operation(summary = "[内部] 检查 JTI 是否在黑名单", hidden = true)
    @GetMapping("/internal/blacklist/{jti}")
    public ApiResponse<Boolean> checkJtiBlacklist(@PathVariable("jti") String jti,
                                                   @RequestHeader("X-Internal-Secret") String secret) {
        // 简单内部鉴权：生产可改为 IP 白名单 + 网关签名
        if (!internalSecret.equals(secret)) {
            return ApiResponse.success(true); // 伪装拒绝
        }
        // 直接使用 AuthServiceImpl 中的方法：通过反射或自定义公开方法调用
        // 这里使用简单实现：复制 logic — 实际上抽成 common redis util 更好
        boolean blacklisted = ((com.yingjianxia.user.service.impl.AuthServiceImpl) authService).isJtiBlacklisted(jti);
        return ApiResponse.success(blacklisted);
    }

    /* ======================== 工具 ======================== */

    private String extractBearer(String header) {
        if (header == null) return null;
        return header.startsWith("Bearer ") ? header.substring(7) : header;
    }

    /**
     * 从 HTTP 请求中提取设备信息（User-Agent + 客户端 IP）
     */
    private DeviceInfo extractDeviceInfo(HttpServletRequest request) {
        DeviceInfo info = new DeviceInfo();
        info.setUserAgent(request.getHeader("User-Agent"));
        // 网关注入的 X-Client-Ip 优先
        String ip = request.getHeader("X-Client-Ip");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        info.setIp(ip);
        return info;
    }
}
