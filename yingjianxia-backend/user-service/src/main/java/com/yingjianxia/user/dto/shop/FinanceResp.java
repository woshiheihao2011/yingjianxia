package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务设置 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "财务设置响应")
public class FinanceResp {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "开户名")
    private String bankAccountName;

    @Schema(description = "银行账号（脱敏）")
    private String bankAccountNo;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "提现阈值（元）")
    private BigDecimal withdrawThreshold;

    @Schema(description = "提现费率")
    private BigDecimal withdrawFeeRate;

    @Schema(description = "结算周期：daily/weekly/monthly")
    private String settlementCycle;
}
