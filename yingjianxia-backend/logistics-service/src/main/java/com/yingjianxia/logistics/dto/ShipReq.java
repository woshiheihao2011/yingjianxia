package com.yingjianxia.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 发货请求（卖家端）
 */
@Data
@Schema(description = "卖家发货请求")
public class ShipReq {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "本次发货商品明细ID列表（支持部分发货）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "发货商品明细不能为空")
    private List<Long> orderItemIds;

    @Schema(description = "快递公司：顺丰/京东/中通/圆通/韵达/EMS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "快递公司不能为空")
    private String expressCompany;

    @Schema(description = "快递公司编码：SF/JD/ZTO/YTO/YUNDA/EMS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "快递公司编码不能为空")
    private String expressCode;

    @Schema(description = "运单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运单号不能为空")
    private String trackingNo;

    @Schema(description = "寄件人姓名")
    private String senderName;

    @Schema(description = "寄件人电话")
    private String senderPhone;

    @Schema(description = "寄件地址")
    private String senderAddress;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人电话")
    private String receiverPhone;

    @Schema(description = "收件地址")
    private String receiverAddress;
}
