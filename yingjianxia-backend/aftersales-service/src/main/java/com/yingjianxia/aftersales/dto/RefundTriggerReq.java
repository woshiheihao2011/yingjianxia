package com.yingjianxia.aftersales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 触发退款请求（aftersales → escrow-service 内部调用）
 */
@Data
@Schema(description = "触发退款请求（内部）")
public class RefundTriggerReq {

    @Schema(description = "售后单ID")
    private Long afterSaleId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String reason;
}
