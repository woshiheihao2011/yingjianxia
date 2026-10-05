package com.yingjianxia.aftersales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后申请 — after_sales 表
 * <p>
 * 状态机：0审核中 → 1已通过 → 2待退货 → 3待退款 → 4已完成 / 5已拒绝 / 6仲裁中 / 7已关闭
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sales")
public class AfterSale extends BaseEntity {

    /** 售后单号 */
    private String asNo;

    /** 关联订单 */
    private Long orderId;

    /** 关联订单明细（可指定商品） */
    private Long orderItemId;

    /** 买家ID */
    private Long buyerId;

    /** 卖家ID */
    private Long sellerId;

    /** 售后类型：1仅退款 2退货退款 3换货 */
    private Integer type;

    /** 申请原因 */
    private String reason;

    /** 问题描述 */
    private String description;

    /** 凭证图片（JSON，最多6张） */
    private String evidenceImages;

    /** 退款金额（≤订单实付） */
    private BigDecimal refundAmount;

    /** 退货运费承担金额 */
    private BigDecimal returnShippingFee;

    /** 运费承担方：1买家 2卖家 */
    private Integer shippingFeeBearer;

    /** 0审核中 1已通过 2待退货 3待退款 4已完成 5已拒绝 6仲裁中 7已关闭 */
    private Integer status;

    /** 卖家处理：NULL待处理 1同意 2拒绝 */
    private Integer sellerResponse;

    /** 卖家拒绝理由 */
    private String sellerRefuseReason;

    /** 卖家备注 */
    private String sellerRemark;

    /** 退货快递公司 */
    private String returnExpress;

    /** 退货运单号 */
    private String returnTrackingNo;

    /** 卖家确认收货时间 */
    private LocalDateTime returnReceivedAt;

    /** 退货地址（JSON 快照） */
    private String returnAddress;

    /** 7天退换截止时间 */
    private LocalDateTime deadline;

    /** 自动处理截止（卖家超时未处理自动同意） */
    private LocalDateTime autoProcessDeadline;

    /** 退款流水号 */
    private String refundTxNo;

    /** 退款完成时间 */
    private LocalDateTime refundAt;

    /** 是否申请平台仲裁 */
    private Boolean arbitrationApplied;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_REVIEWING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_RETURN_PENDING = 2;
    public static final int STATUS_REFUND_PENDING = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_REFUSED = 5;
    public static final int STATUS_ARBITRATING = 6;
    public static final int STATUS_CLOSED = 7;

    /* ========== 类型常量 ========== */
    public static final int TYPE_REFUND_ONLY = 1;
    public static final int TYPE_RETURN_REFUND = 2;
    public static final int TYPE_EXCHANGE = 3;

    /* ========== 卖家处理常量 ========== */
    public static final int SELLER_AGREE = 1;
    public static final int SELLER_REFUSE = 2;

    /* ========== 运费承担方 ========== */
    public static final int FEE_BEARER_BUYER = 1;
    public static final int FEE_BEARER_SELLER = 2;

    /* ========== 操作人类型（与日志/消息共用语义） ========== */
    public static final int OPERATOR_BUYER = 1;
    public static final int OPERATOR_SELLER = 2;
    public static final int OPERATOR_SYSTEM = 3;
    public static final int OPERATOR_ARBITRATOR = 4;
}
