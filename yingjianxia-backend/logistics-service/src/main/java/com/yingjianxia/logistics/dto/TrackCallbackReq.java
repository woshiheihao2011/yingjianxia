package com.yingjianxia.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流推送回调请求（第三方快递公司 / Mock 模拟器推送）
 */
@Data
@Schema(description = "物流轨迹推送回调")
public class TrackCallbackReq {

    @Schema(description = "运单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运单号不能为空")
    private String trackingNo;

    @Schema(description = "状态描述：已揽收/运输中/到达分拣/派送中/已签收/异常", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "地点描述")
    private String location;

    @Schema(description = "详细信息")
    private String description;

    @Schema(description = "快递员姓名（派送时）")
    private String courierName;

    @Schema(description = "快递员电话")
    private String courierPhone;

    @Schema(description = "轨迹时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "轨迹时间不能为空")
    private LocalDateTime trackedAt;

    @Schema(description = "签名（用于回调验签）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名不能为空")
    private String sign;
}
