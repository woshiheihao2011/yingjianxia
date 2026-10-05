package com.yingjianxia.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流轨迹查询响应（含发货记录摘要 + 轨迹列表）
 */
@Data
@Schema(description = "物流轨迹查询响应")
public class TrackQueryResp {

    @Schema(description = "发货记录ID")
    private Long shipmentId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "运单号")
    private String trackingNo;

    @Schema(description = "快递公司")
    private String expressCompany;

    @Schema(description = "快递公司编码")
    private String expressCode;

    @Schema(description = "发货状态：0已发货 1运输中 2派送中 3已签收 4异常")
    private Integer status;

    @Schema(description = "异常原因")
    private String exceptionReason;

    @Schema(description = "发货时间")
    private LocalDateTime shippedAt;

    @Schema(description = "签收时间")
    private LocalDateTime receivedAt;

    @Schema(description = "轨迹列表（按时间正序）")
    private List<LogisticsTrack> tracks;

    @Data
    @Schema(description = "单条物流轨迹")
    public static class LogisticsTrack {
        @Schema(description = "状态描述")
        private String status;

        @Schema(description = "状态码")
        private Integer statusCode;

        @Schema(description = "地点")
        private String location;

        @Schema(description = "详细信息")
        private String description;

        @Schema(description = "快递员姓名")
        private String courierName;

        @Schema(description = "快递员电话")
        private String courierPhone;

        @Schema(description = "轨迹时间")
        private LocalDateTime trackedAt;
    }
}
