package com.yingjianxia.escrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钱包操作锁日志表 — wallet_lock_logs（审计用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wallet_lock_logs")
public class WalletLockLog extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 关联流水号 */
    private String txNo;

    /** 1冻结 2解冻 */
    private Integer lockType;

    /** 金额 */
    private BigDecimal amount;

    /** 原因 */
    private String reason;

    /* ========== 本表不存在：updated_at/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 锁类型常量 ========== */
    public static final int LOCK_FREEZE = 1;
    public static final int LOCK_UNFREEZE = 2;
}
