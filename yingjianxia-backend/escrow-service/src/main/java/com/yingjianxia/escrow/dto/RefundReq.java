package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 退款请求（取消/售后退款，解冻资金退回买家）
 */
@Data
@Schema(description = "退款请求")
public class RefundReq {

    @Schema(description = "担保记录ID（与orderId二选一）")
    private Long escrowId;

    @Schema(description = "订单ID（与escrowId二选一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "关联售后单ID")
    private Long afterSaleId;

    @Schema(description = "幂等键（防重复退款）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
