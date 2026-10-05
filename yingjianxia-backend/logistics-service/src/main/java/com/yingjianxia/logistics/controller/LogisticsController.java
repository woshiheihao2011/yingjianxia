package com.yingjianxia.logistics.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.logistics.dto.ShipReq;
import com.yingjianxia.logistics.dto.TrackCallbackReq;
import com.yingjianxia.logistics.dto.TrackQueryResp;
import com.yingjianxia.logistics.entity.Shipment;
import com.yingjianxia.logistics.service.LogisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流服务 Controller
 */
@Tag(name = "物流服务", description = "卖家发货/买家查轨迹/物流回调推送/自动签收")
@RestController
@RequestMapping({"/api/v1/logistics", "/api/v1/shipments"})
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService service;

    /* ========== 卖家端 ========== */

    @Operation(summary = "[卖家] 创建发货记录（支持部分发货）")
    @PostMapping({"", "/", "/ship"})
    public ApiResponse<Long> ship(@Valid @RequestBody ShipReq req) {
        return ApiResponse.success(service.createShipment(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[卖家] 发货记录列表")
    @GetMapping({"", "/"})
    public ApiResponse<IPage<Shipment>> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(service.listShipments(UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "[卖家] 发货记录详情")
    @GetMapping("/{shipmentId}")
    public ApiResponse<Shipment> detail(@PathVariable Long shipmentId) {
        return ApiResponse.success(service.getShipment(shipmentId));
    }

    @Operation(summary = "[卖家] 运单预览")
    @GetMapping("/{shipmentId}/waybill-preview")
    public ApiResponse<Map<String, Object>> waybillPreview(@PathVariable Long shipmentId) {
        Shipment s = service.getShipment(shipmentId);
        Map<String, Object> data = new HashMap<>();
        data.put("shipmentId", s.getId());
        data.put("trackingNo", s.getTrackingNo());
        data.put("expressCompany", s.getExpressCompany());
        data.put("expressCode", s.getExpressCode());
        data.put("senderName", s.getSenderName());
        data.put("senderPhone", s.getSenderPhone());
        data.put("senderAddress", s.getSenderAddress());
        data.put("receiverName", s.getReceiverName());
        data.put("receiverPhone", s.getReceiverPhone());
        data.put("receiverAddress", s.getReceiverAddress());
        return ApiResponse.success(data);
    }

    @Operation(summary = "[卖家] 运费计算")
    @PostMapping("/calculate-freight")
    public ApiResponse<Map<String, Object>> calculateFreight(@RequestBody Map<String, Object> body) {
        // 简化实现：按重量 * 基础费率
        Object weightObj = body.get("weight");
        BigDecimal weight = weightObj == null ? BigDecimal.ONE : new BigDecimal(String.valueOf(weightObj));
        BigDecimal base = new BigDecimal("8.00");
        BigDecimal freight = base.add(weight.multiply(new BigDecimal("2.00")));
        Map<String, Object> data = new HashMap<>();
        data.put("freight", freight);
        data.put("currency", "CNY");
        data.put("estimatedDays", 3);
        return ApiResponse.success(data);
    }

    @Operation(summary = "[卖家] 查询订单发货记录列表")
    @GetMapping("/seller/orders/{orderId}/shipments")
    public ApiResponse<List<TrackQueryResp>> sellerShipments(@PathVariable Long orderId) {
        return ApiResponse.success(service.listByOrder(orderId, UserContext.requiredUserId()));
    }

    /* ========== 买家端 ========== */

    @Operation(summary = "[买家] 查询物流轨迹（按订单号）")
    @GetMapping("/buyer/orders/{orderId}/tracks")
    public ApiResponse<TrackQueryResp> buyerTracks(@PathVariable Long orderId) {
        return ApiResponse.success(service.queryTracks(orderId, UserContext.requiredUserId()));
    }

    @Operation(summary = "[用户] 查询物流轨迹（按运单号）")
    @GetMapping("/tracks")
    public ApiResponse<TrackQueryResp> tracksByNo(@RequestParam String trackingNo) {
        return ApiResponse.success(service.queryTracksByTrackingNo(trackingNo));
    }

    /* ========== 内部：物流回调推送 ========== */

    @Operation(summary = "[内部] 物流轨迹推送回调（含验签）", hidden = true)
    @PostMapping("/internal/track/callback")
    public ApiResponse<Void> trackCallback(@Valid @RequestBody TrackCallbackReq req) {
        service.handleTrackCallback(req);
        return ApiResponse.success();
    }
}
