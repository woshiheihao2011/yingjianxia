package com.yingjianxia.escrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 担保交易记录表 — escrow_records
 * <p>
 * 一订单一担保。状态机：0待冻结 → 1已冻结 → 2已放款 / 3已退款 / 4退款中
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("escrow_records")
public class EscrowRecord extends BaseEntity {

    /** 关联订单（一订单一担保） */
    private Long orderId;

    /** 订单号（冗余） */
    private String orderNo;

    /** 买家ID */
    private Long buyerId;

    /** 卖家ID */
    private Long sellerId;

    /** 担保金额 */
    private BigDecimal amount;

    /** 平台手续费 */
    private BigDecimal fee;

    /** 手续费率（%） */
    private BigDecimal feeRate;

    /** 实际放款金额（amount - fee） */
    private BigDecimal settleAmount;

    /** 0待冻结 1已冻结 2已放款 3已退款 4退款中 */
    private Integer status;

    /** 支付方式 */
    private String paymentMethod;

    /** 第三方支付单号 */
    private String paymentChannelTx;

    /** 冻结时间 */
    private LocalDateTime frozenAt;

    /** 放款/退款时间 */
    private LocalDateTime releasedAt;

    /** 退款原因 */
    private String refundReason;

    /** 关联售后单ID */
    private Long afterSaleId;

    /** 幂等键 */
    private String idempotencyKey;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    /** 0 待冻结 */
    public static final int STATUS_PENDING_FREEZE = 0;
    /** 1 已冻结 */
    public static final int STATUS_FROZEN = 1;
    /** 2 已放款 */
    public static final int STATUS_RELEASED = 2;
    /** 3 已退款 */
    public static final int STATUS_REFUNDED = 3;
    /** 4 退款中 */
    public static final int STATUS_REFUNDING = 4;
}
