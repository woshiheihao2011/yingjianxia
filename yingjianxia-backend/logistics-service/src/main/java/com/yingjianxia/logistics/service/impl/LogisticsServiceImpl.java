package com.yingjianxia.logistics.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.logistics.dto.ShipReq;
import com.yingjianxia.logistics.dto.TrackCallbackReq;
import com.yingjianxia.logistics.dto.TrackQueryResp;
import com.yingjianxia.logistics.entity.LogisticsTrack;
import com.yingjianxia.logistics.entity.Shipment;
import com.yingjianxia.logistics.enums.LogisticsErrorCode;
import com.yingjianxia.logistics.mapper.LogisticsTrackMapper;
import com.yingjianxia.logistics.mapper.ShipmentMapper;
import com.yingjianxia.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物流服务实现 — 发货记录 + 物流轨迹回调 + 自动签收
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final ShipmentMapper shipmentMapper;
    private final LogisticsTrackMapper trackMapper;

    /** 自动签收阈值：发货后 15 天未签收自动签收 */
    private static final int AUTO_SIGN_DAYS = 15;

    /** 回调验签密钥（生产期通过 Nacos 注入） */
    private static final String CALLBACK_SECRET = "yjx-logistics-callback-secret-2026";

    /** 支持的快递公司编码 */
    private static final Set<String> SUPPORTED_EXPRESS_CODES =
            Set.of("SF", "JD", "ZTO", "YTO", "YUNDA", "EMS");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShipment(ShipReq req, Long sellerId) {
        // 1. 快递公司编码校验
        if (!SUPPORTED_EXPRESS_CODES.contains(req.getExpressCode())) {
            throw new BusinessException(LogisticsErrorCode.EXPRESS_CODE_INVALID);
        }
        // 2. 运单号重复校验
        Long dup = shipmentMapper.selectCount(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getTrackingNo, req.getTrackingNo()));
        if (dup != null && dup > 0) {
            throw new BusinessException(LogisticsErrorCode.TRACKING_NO_DUPLICATED);
        }
        // 3. 收件人快照必填
        if (StrUtil.hasBlank(req.getReceiverName(), req.getReceiverPhone(), req.getReceiverAddress())) {
            throw new BusinessException(LogisticsErrorCode.ORDER_ITEM_IDS_EMPTY);
        }

        // 4. 构造发货记录
        Shipment s = new Shipment();
        s.setOrderId(req.getOrderId());
        s.setSellerId(sellerId);
        s.setOrderItemIds(JSONUtil.toJsonStr(req.getOrderItemIds()));
        s.setExpressCompany(req.getExpressCompany());
        s.setExpressCode(req.getExpressCode());
        s.setTrackingNo(req.getTrackingNo());
        s.setSenderName(req.getSenderName());
        s.setSenderPhone(req.getSenderPhone());
        s.setSenderAddress(req.getSenderAddress());
        s.setReceiverName(req.getReceiverName());
        s.setReceiverPhone(req.getReceiverPhone());
        s.setReceiverAddress(req.getReceiverAddress());
        s.setStatus(Shipment.STATUS_SHIPPED);
        s.setShippedAt(LocalDateTime.now());
        shipmentMapper.insert(s);
        log.info("【发货记录创建】orderId={}, shipmentId={}, trackingNo={}",
                req.getOrderId(), s.getId(), req.getTrackingNo());
        return s.getId();
    }

    @Override
    public List<TrackQueryResp> listByOrder(Long orderId, Long sellerId) {
        List<Shipment> shipments = shipmentMapper.selectList(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getOrderId, orderId)
                .eq(Shipment::getSellerId, sellerId)
                .orderByDesc(Shipment::getShippedAt));
        if (shipments.isEmpty()) {
            throw new BusinessException(LogisticsErrorCode.SHIPMENT_NOT_FOUND);
        }
        return shipments.stream().map(s -> assembleResp(s, false)).collect(Collectors.toList());
    }

    @Override
    public IPage<Shipment> listShipments(Long sellerId, Integer pageNum, Integer pageSize) {
        Page<Shipment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Shipment> w = new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getSellerId, sellerId)
                .orderByDesc(Shipment::getShippedAt);
        return shipmentMapper.selectPage(page, w);
    }

    @Override
    public Shipment getShipment(Long shipmentId) {
        Shipment s = shipmentMapper.selectById(shipmentId);
        if (s == null) {
            throw new BusinessException(LogisticsErrorCode.SHIPMENT_NOT_FOUND);
        }
        return s;
    }

    @Override
    public TrackQueryResp queryTracks(Long orderId, Long buyerId) {
        // 买家端：按订单查最新一条发货记录的轨迹（订单→卖家→收件人对应关系由 order-service 维护，
        // 此处仅按 orderId 查询最新发货，简化实现）
        Shipment s = shipmentMapper.selectOne(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getOrderId, orderId)
                .orderByDesc(Shipment::getShippedAt)
                .last("LIMIT 1"));
        if (s == null) {
            throw new BusinessException(LogisticsErrorCode.SHIPMENT_NOT_FOUND);
        }
        return assembleResp(s, true);
    }

    @Override
    public TrackQueryResp queryTracksByTrackingNo(String trackingNo) {
        Shipment s = shipmentMapper.selectOne(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getTrackingNo, trackingNo)
                .last("LIMIT 1"));
        if (s == null) {
            throw new BusinessException(LogisticsErrorCode.SHIPMENT_NOT_FOUND);
        }
        return assembleResp(s, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTrackCallback(TrackCallbackReq req) {
        // 1. 验签：sign = SHA256(trackingNo + status + trackedAt + secret)
        String expectedSign = SecureUtil.sha256(
                req.getTrackingNo() + req.getStatus() + req.getTrackedAt() + CALLBACK_SECRET);
        if (!expectedSign.equalsIgnoreCase(req.getSign())) {
            throw new BusinessException(LogisticsErrorCode.SIGN_VERIFY_FAIL);
        }

        // 2. 定位发货记录
        Shipment s = shipmentMapper.selectOne(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getTrackingNo, req.getTrackingNo())
                .last("LIMIT 1"));
        if (s == null) {
            throw new BusinessException(LogisticsErrorCode.SHIPMENT_NOT_FOUND);
        }
        if (s.getStatus() == Shipment.STATUS_RECEIVED) {
            log.info("【物流回调】运单已签收，忽略后续推送 trackingNo={}", req.getTrackingNo());
            return;
        }

        // 3. 解析状态码（依据状态描述关键字回映射）
        int statusCode = mapStatusCodeByText(req.getStatus());
        // 4. 插入轨迹记录
        LogisticsTrack t = new LogisticsTrack();
        t.setShipmentId(s.getId());
        t.setStatus(req.getStatus());
        t.setStatusCode(statusCode);
        t.setLocation(req.getLocation());
        t.setDescription(req.getDescription());
        t.setCourierName(req.getCourierName());
        t.setCourierPhone(req.getCourierPhone());
        t.setTrackedAt(req.getTrackedAt());
        trackMapper.insert(t);

        // 5. 状态机推进发货记录状态
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Shipment> uw = new LambdaUpdateWrapper<Shipment>()
                .eq(Shipment::getId, s.getId())
                .set(Shipment::getLastTrackAt, now)
                .set(Shipment::getUpdatedAt, now);

        int newShipmentStatus = mapShipmentStatusByCode(statusCode, s.getStatus());
        if (newShipmentStatus != s.getStatus()) {
            uw.set(Shipment::getStatus, newShipmentStatus);
            if (newShipmentStatus == Shipment.STATUS_RECEIVED) {
                uw.set(Shipment::getReceivedAt, now);
            }
            if (newShipmentStatus == Shipment.STATUS_EXCEPTION) {
                uw.set(Shipment::getExceptionReason, req.getDescription());
            }
        }
        shipmentMapper.update(null, uw);
        log.info("【物流回调】trackingNo={}, trackCode={}, shipmentStatus {}->{}",
                req.getTrackingNo(), statusCode, s.getStatus(), newShipmentStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoSignReceived() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(AUTO_SIGN_DAYS);
        List<Shipment> candidates = shipmentMapper.selectList(new LambdaQueryWrapper<Shipment>()
                .in(Shipment::getStatus, Shipment.STATUS_SHIPPED, Shipment.STATUS_IN_TRANSIT, Shipment.STATUS_DELIVERING)
                .lt(Shipment::getShippedAt, threshold));
        if (candidates.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (Shipment s : candidates) {
            shipmentMapper.update(null, new LambdaUpdateWrapper<Shipment>()
                    .eq(Shipment::getId, s.getId())
                    .set(Shipment::getStatus, Shipment.STATUS_RECEIVED)
                    .set(Shipment::getReceivedAt, now)
                    .set(Shipment::getLastTrackAt, now)
                    .set(Shipment::getUpdatedAt, now));
            // 插入一条系统签收轨迹
            LogisticsTrack t = new LogisticsTrack();
            t.setShipmentId(s.getId());
            t.setStatus("已签收");
            t.setStatusCode(LogisticsTrack.CODE_RECEIVED);
            t.setDescription("超时自动签收");
            t.setTrackedAt(now);
            trackMapper.insert(t);
        }
        log.info("【自动签收】共处理 {} 条发货记录", candidates.size());
    }

    /* ======================== 内部工具 ======================== */

    private TrackQueryResp assembleResp(Shipment s, boolean withTracks) {
        TrackQueryResp resp = new TrackQueryResp();
        resp.setShipmentId(s.getId());
        resp.setOrderId(s.getOrderId());
        resp.setTrackingNo(s.getTrackingNo());
        resp.setExpressCompany(s.getExpressCompany());
        resp.setExpressCode(s.getExpressCode());
        resp.setStatus(s.getStatus());
        resp.setExceptionReason(s.getExceptionReason());
        resp.setShippedAt(s.getShippedAt());
        resp.setReceivedAt(s.getReceivedAt());

        if (withTracks) {
            List<LogisticsTrack> tracks = trackMapper.selectList(new LambdaQueryWrapper<LogisticsTrack>()
                    .eq(LogisticsTrack::getShipmentId, s.getId())
                    .orderByAsc(LogisticsTrack::getTrackedAt));
            List<TrackQueryResp.LogisticsTrack> list = new ArrayList<>(tracks.size());
            for (LogisticsTrack t : tracks) {
                TrackQueryResp.LogisticsTrack r = new TrackQueryResp.LogisticsTrack();
                r.setStatus(t.getStatus());
                r.setStatusCode(t.getStatusCode());
                r.setLocation(t.getLocation());
                r.setDescription(t.getDescription());
                r.setCourierName(t.getCourierName());
                r.setCourierPhone(t.getCourierPhone());
                r.setTrackedAt(t.getTrackedAt());
                list.add(r);
            }
            resp.setTracks(list);
        } else {
            resp.setTracks(new ArrayList<>());
        }
        return resp;
    }

    /**
     * 依据轨迹状态文本映射状态码
     */
    private int mapStatusCodeByText(String statusText) {
        if (statusText == null) return LogisticsTrack.CODE_IN_TRANSIT;
        if (statusText.contains("揽收")) return LogisticsTrack.CODE_PICKED;
        if (statusText.contains("到达") || statusText.contains("分拣")) return LogisticsTrack.CODE_ARRIVED;
        if (statusText.contains("派送") || statusText.contains("派件")) return LogisticsTrack.CODE_DELIVERING;
        if (statusText.contains("签收") || statusText.contains("已收")) return LogisticsTrack.CODE_RECEIVED;
        if (statusText.contains("异常") || statusText.contains("退回")) return LogisticsTrack.CODE_EXCEPTION;
        return LogisticsTrack.CODE_IN_TRANSIT;
    }

    /**
     * 依据轨迹状态码推进发货记录状态机
     */
    private int mapShipmentStatusByCode(int trackCode, int currentStatus) {
        // 已签收状态为终态，不再变更
        switch (trackCode) {
            case LogisticsTrack.CODE_PICKED:
            case LogisticsTrack.CODE_IN_TRANSIT:
            case LogisticsTrack.CODE_ARRIVED:
                // 揽收/运输/到达 → 运输中
                return Math.max(currentStatus, Shipment.STATUS_IN_TRANSIT);
            case LogisticsTrack.CODE_DELIVERING:
                return Shipment.STATUS_DELIVERING;
            case LogisticsTrack.CODE_RECEIVED:
                return Shipment.STATUS_RECEIVED;
            case LogisticsTrack.CODE_EXCEPTION:
                return Shipment.STATUS_EXCEPTION;
            default:
                return Optional.of(currentStatus)
                        .filter(s -> s == Shipment.STATUS_RECEIVED || s == Shipment.STATUS_EXCEPTION)
                        .orElse(Shipment.STATUS_IN_TRANSIT);
        }
    }
}
