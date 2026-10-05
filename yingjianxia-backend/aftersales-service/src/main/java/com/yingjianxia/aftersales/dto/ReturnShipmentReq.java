package com.yingjianxia.aftersales.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 买家填写退货物流请求
 */
@Data
@Schema(description = "填写退货物流")
public class ReturnShipmentReq {

    @Schema(description = "售后单ID（路径参数传入，可为空）")
    private Long afterSaleId;

    @Schema(description = "退货快递公司", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退货快递公司不能为空")
    @JsonAlias({"express", "expressCompany"})
    private String returnExpress;

    @Schema(description = "退货运单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退货运单号不能为空")
    @JsonAlias({"trackingNo", "trackingNumber"})
    private String returnTrackingNo;
}
