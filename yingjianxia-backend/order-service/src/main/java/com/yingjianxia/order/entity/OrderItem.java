package com.yingjianxia.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细表 — order_items
 * <p>
 * product_snapshot 为商品快照 JSON（标题/图片/规格/价格，防修改）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_items")
public class OrderItem extends BaseEntity {

    /** 关联订单 */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品快照（标题/图片/规格/价格，防修改） */
    private String productSnapshot;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal subtotal;

    /** 发货状态：0未发货 1已发货 2已签收 */
    private Integer shipmentStatus;

    /* ========== createdAt/updatedAt/deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 发货状态常量 ========== */
    public static final int SHIP_NOT_SHIPPED = 0;
    public static final int SHIP_SHIPPED = 1;
    public static final int SHIP_SIGNED = 2;
}
