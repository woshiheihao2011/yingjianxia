package com.yingjianxia.common.core.constants;

import java.math.BigDecimal;

/**
 * 全局通用业务常量
 *
 * @author 硬件侠后端团队
 */
public interface BusinessConstants {

    /* ---------- 通用开关 ---------- */
    int TRUE  = 1;
    int FALSE = 0;

    /* ---------- 通用状态（status字段，1启用 0 禁用） ---------- */
    int STATUS_ENABLED  = 1;
    int STATUS_DISABLED = 0;
    int STATUS_DELETED  = -1; // 逻辑删除

    /* ---------- 删除标记（MyBatis Plus 逻辑删除） ---------- */
    int DELETED_NO = 0;
    int DELETED_YES = 1;

    /* ---------- 角色编码 ---------- */
    String ROLE_BUYER     = "BUYER";       // 买家
    String ROLE_SELLER    = "SELLER";      // 卖家
    String ROLE_INSPECTOR = "INSPECTOR";   // 验机工程师
    String ROLE_AUDITOR   = "AUDITOR";     // 审核员
    String ROLE_CS        = "CS";          // 客服
    String ROLE_ADMIN     = "ADMIN";       // 平台管理员

    /* ---------- SMS 发送场景 ---------- */
    String SMS_SCENE_REGISTER    = "REGISTER";
    String SMS_SCENE_LOGIN       = "LOGIN";
    String SMS_SCENE_RESET_PWD   = "RESET_PWD";
    String SMS_SCENE_BIND_PHONE  = "BIND_PHONE";
    String SMS_SCENE_WITHDRAW    = "WITHDRAW";

    /* ---------- 订单超时（单位：毫秒） ---------- */
    long ORDER_PAY_TIMEOUT_MS       = 30L * 60 * 1000;   // 待付款超时 30min
    long ORDER_AUTO_CONFIRM_MS      = 7L * 24 * 3600 * 1000; // 待收货自动确认 7天
    long INSPECTION_TIMEOUT_MS      = 48L * 3600 * 1000;  // 验机 48h 未完成预警
    long AFTERSALES_AUDIT_TIMEOUT_MS  = 72L * 3600 * 1000; // 售后 72h 未审核自动通过
    long AFTERSALES_RETURN_TIMEOUT_MS = 7L * 24 * 3600 * 1000; // 买家 7 天未填写物流

    /* ---------- 交易费率 ---------- */
    BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.01"); // 平台抽佣 1%

    /* ---------- 金额单位 ---------- */
    /** 系统内部金额单位：分 */
    int CURRENCY_SCALE = 2;

    /* ---------- 分页默认值 ---------- */
    long DEFAULT_PAGE_NUM = 1L;
    long DEFAULT_PAGE_SIZE = 10L;
    long MAX_PAGE_SIZE = 500L;

    /* ---------- 幂等键 TTL（秒） ---------- */
    int IDEMPOTENT_DEFAULT_TTL_SECONDS = 60;

    /* ---------- HTTP Header 常量 ---------- */
    String HDR_USER_ID       = "X-User-Id";
    String HDR_USER_ROLES    = "X-User-Roles";
    String HDR_IDEMPOTENT    = "X-Idempotency-Key";
    String HDR_TRACE_ID      = "X-Trace-Id";
    String HDR_GRAY_FLAG     = "X-Gray-Flag"; // 灰度发布标记，取值 gray 走灰度实例
    String HDR_REAL_IP       = "X-Real-IP";
    String HDR_FORWARDED_FOR = "X-Forwarded-For";

    /* ---------- API 前缀（网关路由使用） ---------- */
    String API_PREFIX = "/api/v1";
}
