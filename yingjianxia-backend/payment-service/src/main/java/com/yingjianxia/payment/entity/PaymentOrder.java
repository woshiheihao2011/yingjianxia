package com.yingjianxia.payment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单 — payment_orders 表
 * <p>
 * 与 escrow-service 共享 db_escrow 数据库。本表专管第三方渠道支付单
 * （微信/支付宝统一下单 + 回调入账），区别于 recharge_records（钱包充值）。
 * <p>
 * 状态机：0待支付 → 1已支付 → 2已关闭（超时/手动关单）
 *                       └──── → 3已退款
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_orders")
public class PaymentOrder extends BaseEntity {

    /** 支付单号（业务唯一） */
    private String paymentNo;

    /** 关联业务订单ID */
    private Long orderId;

    /** 支付用户ID */
    private Long userId;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 支付渠道：wechat / alipay */
    private String channel;

    /** 第三方渠道订单号（统一下单返回） */
    private String channelOrderNo;

    /** 0待支付 1已支付 2已关闭 3已退款 */
    private Integer status;

    /** 支付单过期时间（超时自动关单） */
    private LocalDateTime expireAt;

    /** 实际支付成功时间 */
    private LocalDateTime paidAt;

    /** 原始回调数据（JSON，审计追溯） */
    private String callbackRaw;

    /** 幂等键（防重复创建/重复回调） */
    private String idempotencyKey;

    /* ========== deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 状态常量 ========== */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CLOSED = 2;
    public static final int STATUS_REFUNDED = 3;

    /* ========== 渠道常量 ========== */
    public static final String CHANNEL_WECHAT = "wechat";
    public static final String CHANNEL_ALIPAY = "alipay";
    public static final String CHANNEL_WALLET = "wallet";
}
