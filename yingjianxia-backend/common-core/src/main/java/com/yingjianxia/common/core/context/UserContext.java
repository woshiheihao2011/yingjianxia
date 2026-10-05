package com.yingjianxia.common.core.context;

import lombok.Data;

/**
 * 请求上下文 — 存储当前请求的用户信息和链路信息
 * <p>
 * 由 common-web 的 {@code RequestContextInterceptor} 在请求进入时解析
 * 网关注入的 X-User-Id / X-User-Roles / X-Trace-Id，并放入 ThreadLocal。
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Data
public class UserContext {

    /**
     * 当前登录用户ID；未登录为 null
     */
    private Long userId;

    /**
     * 当前用户角色，逗号分隔
     */
    private String roles;

    /**
     * 当前请求追踪ID
     */
    private String traceId;

    /**
     * 客户端真实IP
     */
    private String clientIp;

    /**
     * 是否灰度流量
     */
    private boolean gray;

    /* ========== ThreadLocal 管理 ========== */
    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        UserContext ctx = HOLDER.get();
        if (ctx == null) {
            ctx = new UserContext();
            HOLDER.set(ctx);
        }
        return ctx;
    }

    public static void clear() {
        HOLDER.remove();
    }

    /* ========== 便捷读取 ========== */
    public static Long currentUserId() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getUserId();
    }

    public static Long requiredUserId() {
        Long uid = currentUserId();
        if (uid == null) {
            throw new IllegalStateException("当前请求未携带用户身份信息");
        }
        return uid;
    }

    public static String currentTraceId() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getTraceId();
    }
}
