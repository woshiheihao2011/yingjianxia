package com.yingjianxia.user.service;

import com.yingjianxia.user.dto.auth.*;

import java.util.List;

/**
 * 认证服务接口 — 注册/登录/验证码/JWT/登录设备管理
 *
 * @author 硬件侠后端团队
 */
public interface AuthService {

    /**
     * 发送短信验证码
     *
     * @param req 手机号 + 场景
     */
    void sendSmsCode(SendSmsCodeReq req);

    /**
     * 手机号注册（验证码+密码）
     *
     * @param req 注册请求
     * @return 登录态（直接登录）
     */
    LoginResp phoneRegister(PhoneRegisterReq req);

    /**
     * 密码登录（手机号+密码）
     *
     * @param req 登录请求
     * @return 双 Token
     */
    LoginResp passwordLogin(PasswordLoginReq req);

    /**
     * 验证码登录（手机号+短信码）
     *
     * @param req 登录请求
     * @return 双 Token
     */
    LoginResp smsCodeLogin(SmsCodeLoginReq req);

    /**
     * 重置密码（短信验证码验证后设置新密码）
     *
     * @param phone       手机号
     * @param smsCode     短信验证码
     * @param newPassword 新密码
     */
    void resetPassword(String phone, String smsCode, String newPassword);

    /**
     * 第三方 OAuth2 登录（微信/QQ/支付宝）
     *
     * @param req 授权请求
     * @return 双 Token（如首次绑定未完成则返回临时态，前端引导补手机号）
     */
    LoginResp oauthLogin(OAuthLoginReq req);

    /**
     * 刷新 Access Token
     *
     * @param req 携带 Refresh Token
     * @return 新的双 Token（Refresh Token 可滚动续签）
     */
    LoginResp refreshToken(RefreshTokenReq req);

    /**
     * 登出 — 将当前 Access Token 及 Refresh Token 放入黑名单
     *
     * @param accessToken  当前请求的 Access Token（从 Header 中取）
     * @param refreshToken 用户上送的 Refresh Token（可选）
     */
    void logout(String accessToken, String refreshToken);

    /* ============================================================
     *  登录设备管理
     * ============================================================ */

    /**
     * 记录登录设备会话（登录/注册/刷新成功后调用）
     *
     * @param userId      用户ID
     * @param accessToken 刚签发的 Access Token
     * @param deviceInfo  设备信息（UA + IP）
     */
    void recordDeviceSession(Long userId, String accessToken, DeviceInfo deviceInfo);

    /**
     * 查询当前用户的所有登录设备列表
     *
     * @param userId     用户ID
     * @param currentJti 当前请求的 JTI（用于标记是否当前设备）
     * @return 设备列表（已过滤过期和已登出设备）
     */
    List<LoginDeviceResp> listDevices(Long userId, String currentJti);

    /**
     * 踢出指定设备（将对应 JTI 加入黑名单并删除活跃记录）
     *
     * @param userId 用户ID
     * @param jti    目标设备 Token 的 JTI
     */
    void kickDevice(Long userId, String jti);

    /**
     * 退出所有其他设备（保留当前 Token）
     *
     * @param userId     用户ID
     * @param currentJti 当前请求的 JTI
     */
    void kickAllOtherDevices(Long userId, String currentJti);
}
