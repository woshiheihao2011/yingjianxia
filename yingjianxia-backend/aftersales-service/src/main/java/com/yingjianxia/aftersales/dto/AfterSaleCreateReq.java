package com.yingjianxia.aftersales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申请售后请求（买家端）
 */
@Data
@Schema(description = "买家申请售后")
public class AfterSaleCreateReq {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单明细ID（换货必填）")
    private Long orderItemId;

    @Schema(description = "售后类型：1仅退款 2退货退款 3换货", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "售后类型不能为空")
    private Integer type;

    @Schema(description = "申请原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "申请原因不能为空")
    private String reason;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题描述不能为空")
    private String description;

    @Schema(description = "凭证图片（最多6张）")
    @Size(max = 6, message = "凭证图片最多6张")
    private List<String> evidenceImages;

    @Schema(description = "退款金额（≤订单实付）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    @Schema(description = "卖家ID（由订单服务回填，前端不传）", hidden = true)
    private Long sellerId;
}
