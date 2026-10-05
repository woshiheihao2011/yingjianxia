package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务设置 更新 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "财务设置请求")
public class FinanceReq {

    @Schema(description = "开户名")
    private String bankAccountName;

    @Schema(description = "银行账号")
    private String bankAccountNo;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "提现阈值（元）")
    private BigDecimal withdrawThreshold;

    @Schema(description = "提现费率（如 0.006 表示 0.6%）")
    private BigDecimal withdrawFeeRate;

    @Schema(description = "结算周期：daily/weekly/monthly")
    @Pattern(regexp = "^(daily|weekly|monthly)$", message = "结算周期不合法")
    private String settlementCycle;
}
