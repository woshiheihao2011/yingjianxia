package com.yingjianxia.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表 — orders 表
 * <p>
 * 状态机：0待付款 → 1待发货 → 2待收货 → 3已完成 / 4已取消 / 5售后中
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {

    /** 订单号（展示用） */
    private String orderNo;

    /** 买家ID */
    private Long buyerId;

    /** 卖家ID */
    private Long sellerId;

    /** 店铺ID */
    private Long shopId;

    /** 收货地址ID */
    private Long addressId;

    /** 收货地址快照（防修改） */
    private String addressSnapshot;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 优惠减免 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payableAmount;

    /** 使用的优惠券ID */
    private Long couponId;

    /** 使用积分抵扣 */
    private Integer pointsUsed;

    /** 0待付款 1待发货 2待收货 3已完成 4已取消 5售后中 */
    private Integer status;

    /** 支付方式：balance/wechat/alipay */
    private String paymentMethod;

    /** 付款时间 */
    private LocalDateTime paidAt;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 确认收货时间 */
    private LocalDateTime receivedAt;

    /** 自动确认收货截止时间 */
    private LocalDateTime autoConfirmDeadline;

    /** 超时自动取消时间 */
    private LocalDateTime autoCancelDeadline;

    /** 买家留言 */
    private String remark;

    /** 取消原因 */
    private String cancelReason;

    /** 幂等键 */
    private String idempotencyKey;

    /* ========== deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 非持久化：订单明细 ========== */
    @TableField(exist = false)
    private List<OrderItem> items;

    /* ========== 状态常量 ========== */
    /** 0 待付款 */
    public static final int STATUS_PENDING_PAY = 0;
    /** 1 待发货 */
    public static final int STATUS_PENDING_SHIP = 1;
    /** 2 待收货 */
    public static final int STATUS_PENDING_RECEIVE = 2;
    /** 3 已完成 */
    public static final int STATUS_COMPLETED = 3;
    /** 4 已取消 */
    public static final int STATUS_CANCELLED = 4;
    /** 5 售后中 */
    public static final int STATUS_AFTERSALE = 5;

    /* ========== 操作人类型常量 ========== */
    public static final int OP_BUYER = 1;
    public static final int OP_SELLER = 2;
    public static final int OP_SYSTEM = 3;
    public static final int OP_SERVICE = 4;
}
