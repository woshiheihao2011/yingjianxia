package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 担保冻结请求（下单时冻结买家资金）
 */
@Data
@Schema(description = "担保冻结请求")
public class EscrowFreezeReq {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "买家ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long buyerId;

    @Schema(description = "卖家ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long sellerId;

    @Schema(description = "担保金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @DecimalMin(value = "0.01", message = "冻结金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "支付方式：balance/wechat/alipay")
    private String paymentMethod;

    @Schema(description = "第三方支付单号")
    private String paymentChannelTx;

    @Schema(description = "幂等键（防重复冻结）")
    private String idempotencyKey;
}
