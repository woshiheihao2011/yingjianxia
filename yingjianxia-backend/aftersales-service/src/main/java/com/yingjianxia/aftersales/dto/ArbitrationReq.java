package com.yingjianxia.aftersales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台仲裁请求（客服端）
 */
@Data
@Schema(description = "平台仲裁请求")
public class ArbitrationReq {

    @Schema(description = "售后单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "售后单ID不能为空")
    private Long afterSaleId;

    @Schema(description = "仲裁结果：1支持买家(退款) 2支持卖家 3部分退款", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "仲裁结果不能为空")
    private Integer result;

    @Schema(description = "裁定退款金额（部分退款必填）")
    private BigDecimal refundAmount;

    @Schema(description = "仲裁理由", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仲裁理由不能为空")
    private String reason;

    @Schema(description = "证据摘要")
    private String evidenceSummary;
}
