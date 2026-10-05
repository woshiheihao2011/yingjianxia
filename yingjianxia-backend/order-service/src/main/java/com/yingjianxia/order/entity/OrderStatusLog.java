package com.yingjianxia.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单状态变更日志 — order_status_logs
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_status_logs")
public class OrderStatusLog extends BaseEntity {

    /** 订单ID */
    private Long orderId;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 操作人ID（系统操作为NULL） */
    private Long operatorId;

    /** 1买家 2卖家 3系统 4客服 */
    private Integer operatorType;

    /** 变更说明 */
    private String remark;

    /* ========== updatedAt/deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 操作人类型常量 ========== */
    public static final int OP_BUYER = 1;
    public static final int OP_SELLER = 2;
    public static final int OP_SYSTEM = 3;
    public static final int OP_SERVICE = 4;
}
