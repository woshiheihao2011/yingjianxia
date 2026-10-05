package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值请求
 */
@Data
@Schema(description = "充值请求")
public class RechargeReq {

    @Schema(description = "充值金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "渠道：wechat/alipay", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "充值渠道不能为空")
    private String channel;

    @Schema(description = "幂等键（防重复充值）")
    private String idempotencyKey;
}
