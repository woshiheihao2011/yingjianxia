package com.yingjianxia.escrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 交易流水表 — transactions
 * <p>
 * 全链路资金审计。type 1~7：收入/支出/退款/充值/提现/冻结/解冻。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("transactions")
public class Transaction extends BaseEntity {

    /** 流水号 */
    private String txNo;

    /** 所属用户 */
    private Long userId;

    /** 1收入 2支出 3退款 4充值 5提现 6冻结 7解冻 */
    private Integer type;

    /** 子类型：order_payment/order_refund/escrow_release/... */
    private String subType;

    /** 金额（正数） */
    private BigDecimal amount;

    /** 1收入(+) 2支出(-) */
    private Integer direction;

    /** 变动前可用余额 */
    private BigDecimal balanceBefore;

    /** 变动后可用余额 */
    private BigDecimal balanceAfter;

    /** 变动前冻结余额 */
    private BigDecimal frozenBefore;

    /** 变动后冻结余额 */
    private BigDecimal frozenAfter;

    /** 关联订单（如有） */
    private Long orderId;

    /** 关联担保记录 */
    private Long escrowId;

    /** 交易对手方用户ID */
    private Long counterpartyId;

    /** 支付方式 */
    private String paymentMethod;

    /** 第三方渠道流水号 */
    private String channelTxNo;

    /** 手续费 */
    private BigDecimal fee;

    /** 描述 */
    private String remark;

    /** 幂等键 */
    private String idempotencyKey;

    /* ========== 本表不存在：updated_at/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 类型常量 ========== */
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;
    public static final int TYPE_REFUND = 3;
    public static final int TYPE_RECHARGE = 4;
    public static final int TYPE_WITHDRAW = 5;
    public static final int TYPE_FREEZE = 6;
    public static final int TYPE_UNFREEZE = 7;

    /* ========== 方向常量 ========== */
    public static final int DIRECTION_IN = 1;
    public static final int DIRECTION_OUT = 2;
}
