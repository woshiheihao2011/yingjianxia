package com.yingjianxia.aftersales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 卖家处理售后请求（卖家端）
 */
@Data
@Schema(description = "卖家处理售后")
public class SellerHandleReq {

    @Schema(description = "售后单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "售后单ID不能为空")
    private Long afterSaleId;

    @Schema(description = "处理结果：1同意 2拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "卖家处理结果不能为空")
    private Integer agree;

    @Schema(description = "拒绝理由（拒绝时必填）")
    private String refuseReason;

    @Schema(description = "卖家备注")
    private String remark;

    @Schema(description = "退货运费承担方：1买家 2卖家（同意退货退款时可指定）")
    private Integer shippingFeeBearer;

    @Schema(description = "退货地址（同意退货退款时回填）")
    private String returnAddress;
}
