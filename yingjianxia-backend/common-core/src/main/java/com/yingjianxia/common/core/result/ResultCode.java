package com.yingjianxia.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用结果码枚举（1 ~ 999）
 * <p>
 * 业务错误码请在各微服务中定义专用枚举（实现 IErrorCode 接口）。
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum ResultCode implements IErrorCode {

    /* ========== 0 : 成功 ========== */
    SUCCESS(0, "操作成功"),

    /* ========== 1 ~ 99 : 基础失败 ========== */
    FAILURE(1, "操作失败"),
    SYSTEM_ERROR(2, "系统异常，请稍后重试"),
    PARAM_ERROR(3, "参数校验失败"),
    PARAM_BIND_ERROR(4, "参数绑定异常"),

    /* ========== 100 ~ 199 : 认证 / 鉴权 ========== */
    UNAUTHORIZED(101, "未登录或Token已失效，请重新登录"),
    TOKEN_EXPIRED(102, "Token已过期"),
    TOKEN_INVALID(103, "Token非法或已被吊销"),
    FORBIDDEN(104, "无权限访问该资源"),
    LOGIN_ERROR(105, "账号或密码错误"),
    ACCOUNT_LOCKED(106, "账号已被锁定，请稍后重试或联系客服"),
    ACCOUNT_DISABLED(107, "账号已被禁用"),
    PHONE_NOT_REGISTERED(108, "手机号未注册"),
    PHONE_ALREADY_REGISTERED(109, "手机号已被注册"),

    /* ========== 200 ~ 299 : 请求类错误 ========== */
    REQUEST_METHOD_NOT_SUPPORTED(201, "不支持的请求方法"),
    REQUEST_CONTENT_TYPE_NOT_SUPPORTED(202, "不支持的Content-Type"),
    REQUEST_BODY_MISSING(203, "请求体缺失"),
    REQUEST_FREQUENT(204, "请求过于频繁，请稍后再试"),
    IDEMPOTENT_KEY_DUPLICATE(205, "重复请求，请勿重复提交"),
    SERVICE_DOWNGRADE(206, "服务暂时不可用，已降级处理"),
    SERVICE_BLOCKED(207, "请求触发限流规则，已被阻断"),

    /* ========== 300 ~ 399 : 资源 / 数据类错误 ========== */
    DATA_NOT_FOUND(301, "数据不存在"),
    DATA_ALREADY_EXISTS(302, "数据已存在"),
    DATA_CONFLICT(303, "数据版本冲突，请刷新后重试"),
    DATA_INTEGRITY_VIOLATION(304, "数据完整性约束冲突"),
    UPLOAD_FILE_EMPTY(305, "上传文件不能为空"),
    UPLOAD_FILE_TOO_LARGE(306, "上传文件超过大小限制"),
    UPLOAD_FILE_TYPE_NOT_ALLOWED(307, "上传文件格式不支持"),

    /* ========== 400 ~ 499 : 第三方 / 外部调用 ========== */
    THIRD_PARTY_CALL_FAIL(401, "第三方服务调用失败"),
    THIRD_PARTY_TIMEOUT(402, "第三方服务调用超时"),
    SIGNATURE_VERIFY_FAIL(403, "签名验证失败"),
    PAYMENT_CHANNEL_ERROR(404, "支付通道异常");

    /* ========== 字段 & 构造 ========== */
    private final Integer code;
    private final String message;
}
