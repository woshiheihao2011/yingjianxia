package com.yingjianxia.escrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录表 — withdrawal_records
 * <p>
 * T+1 到账。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("withdrawal_records")
public class WithdrawalRecord extends BaseEntity {

    /** 提现单号 */
    private String withdrawNo;

    /** 用户ID */
    private Long userId;

    /** 提现金额 */
    private BigDecimal amount;

    /** 提现手续费 */
    private BigDecimal fee;

    /** 实际到账金额 */
    private BigDecimal actualAmount;

    /** 渠道：bank_card/alipay */
    private String channel;

    /** 目标账户（加密存储） */
    private String targetAccount;

    /** 收款人姓名 */
    private String targetName;

    /** 0处理中 1成功 2失败 */
    private Integer status;

    /** 预计到账时间（T+1） */
    private LocalDateTime arriveAt;

    /** 实际到账时间 */
    private LocalDateTime successAt;

    /** 失败原因 */
    private String failedReason;

    /** 幂等键 */
    private String idempotencyKey;

    /* ========== 本表不存在：updated_at/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 2;
}
