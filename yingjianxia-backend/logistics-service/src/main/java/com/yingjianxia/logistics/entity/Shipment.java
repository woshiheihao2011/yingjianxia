package com.yingjianxia.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 发货记录 — shipments 表
 * <p>
 * 状态机：0已发货 → 1运输中 → 2派送中 → 3已签收 → 4异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shipments")
public class Shipment extends BaseEntity {

    /** 关联订单 */
    private Long orderId;

    /** 发货商品明细ID列表（JSON，支持部分发货） */
    private String orderItemIds;

    /** 发货卖家 */
    private Long sellerId;

    /** 快递公司：顺丰/京东/中通/圆通/韵达/EMS */
    private String expressCompany;

    /** 快递公司编码：SF/JD/ZTO/YTO/YUNDA/EMS */
    private String expressCode;

    /** 运单号 */
    private String trackingNo;

    /** 寄件人姓名 */
    private String senderName;

    /** 寄件人电话 */
    private String senderPhone;

    /** 寄件地址 */
    private String senderAddress;

    /** 收件人姓名（快照） */
    private String receiverName;

    /** 收件人电话（快照，加密） */
    private String receiverPhone;

    /** 收件地址（快照） */
    private String receiverAddress;

    /** 0已发货 1运输中 2派送中 3已签收 4异常 */
    private Integer status;

    /** 异常原因 */
    private String exceptionReason;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 最后轨迹更新时间 */
    private LocalDateTime lastTrackAt;

    /** 签收时间 */
    private LocalDateTime receivedAt;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_SHIPPED = 0;
    public static final int STATUS_IN_TRANSIT = 1;
    public static final int STATUS_DELIVERING = 2;
    public static final int STATUS_RECEIVED = 3;
    public static final int STATUS_EXCEPTION = 4;
}
