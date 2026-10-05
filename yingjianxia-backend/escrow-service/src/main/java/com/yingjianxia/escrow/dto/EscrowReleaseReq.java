package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 担保放款请求（确认收货时放款给卖家，扣除手续费）
 */
@Data
@Schema(description = "担保放款请求")
public class EscrowReleaseReq {

    @Schema(description = "担保记录ID（与orderId二选一）")
    private Long escrowId;

    @Schema(description = "订单ID（与escrowId二选一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "操作人ID（系统操作可为空）")
    private Long operatorId;

    @Schema(description = "幂等键（防重复放款）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
