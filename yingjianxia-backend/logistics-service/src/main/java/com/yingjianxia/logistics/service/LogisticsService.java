package com.yingjianxia.logistics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.logistics.dto.ShipReq;
import com.yingjianxia.logistics.dto.TrackCallbackReq;
import com.yingjianxia.logistics.dto.TrackQueryResp;
import com.yingjianxia.logistics.entity.Shipment;

import java.util.List;

/**
 * 物流服务接口
 */
public interface LogisticsService {

    /* ====== 卖家端 ====== */

    /**
     * 创建发货记录（支持部分发货）
     */
    Long createShipment(ShipReq req, Long sellerId);

    /**
     * 卖家查询某订单的发货记录列表
     */
    List<TrackQueryResp> listByOrder(Long orderId, Long sellerId);

    /**
     * 卖家发货记录列表
     */
    IPage<Shipment> listShipments(Long sellerId, Integer pageNum, Integer pageSize);

    /**
     * 发货记录详情
     */
    Shipment getShipment(Long shipmentId);

    /* ====== 买家端 ====== */

    /**
     * 买家查询物流轨迹（按订单ID）
     */
    TrackQueryResp queryTracks(Long orderId, Long buyerId);

    /**
     * 查询物流轨迹（按运单号）
     */
    TrackQueryResp queryTracksByTrackingNo(String trackingNo);

    /* ====== 内部：物流回调 ====== */

    /**
     * 物流轨迹推送回调处理（验签 + 更新发货状态 + 插入轨迹）
     */
    void handleTrackCallback(TrackCallbackReq req);

    /* ====== 定时任务 ====== */

    /**
     * 自动签收定时任务：发货 N 天后未签收自动标记已签收
     */
    void autoSignReceived();
}
