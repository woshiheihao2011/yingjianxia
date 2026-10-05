package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提现请求（T+1到账）
 */
@Data
@Schema(description = "提现请求")
public class WithdrawReq {

    @Schema(description = "提现金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "1.00", message = "提现金额最低1元")
    private BigDecimal amount;

    @Schema(description = "渠道：bank_card/alipay", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提现渠道不能为空")
    private String channel;

    @Schema(description = "目标账户（加密存储）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标账户不能为空")
    private String targetAccount;

    @Schema(description = "收款人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收款人姓名不能为空")
    private String targetName;

    @Schema(description = "幂等键（防重复提现）")
    private String idempotencyKey;
}
