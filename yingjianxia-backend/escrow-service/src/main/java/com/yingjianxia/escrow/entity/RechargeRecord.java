package com.yingjianxia.escrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录表 — recharge_records
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recharge_records")
public class RechargeRecord extends BaseEntity {

    /** 充值单号 */
    private String rechargeNo;

    /** 用户ID */
    private Long userId;

    /** 充值金额 */
    private BigDecimal amount;

    /** 渠道：wechat/alipay */
    private String channel;

    /** 第三方订单号 */
    private String channelOrder;

    /** 0待支付 1成功 2失败 3已关闭 */
    private Integer status;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 失败原因 */
    private String failedReason;

    /** 幂等键 */
    private String idempotencyKey;

    /* ========== 本表不存在：updated_at/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 2;
    public static final int STATUS_CLOSED = 3;
}
