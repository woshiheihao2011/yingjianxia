package com.yingjianxia.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付单请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "创建支付单请求")
public class CreatePaymentReq {

    @Schema(description = "业务订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "支付金额（元，可选，不传则从订单读取）", example = "99.00")
    private BigDecimal amount;

    @Schema(description = "支付渠道：wechat / alipay / wallet", defaultValue = "wallet")
    private String channel = "wallet";

    @Schema(description = "幂等键（可选，防重复创建）")
    private String idempotencyKey;
}
