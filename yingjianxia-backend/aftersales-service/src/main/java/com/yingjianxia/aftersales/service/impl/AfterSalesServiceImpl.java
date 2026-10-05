package com.yingjianxia.aftersales.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.aftersales.dto.*;
import com.yingjianxia.aftersales.entity.AfterSale;
import com.yingjianxia.aftersales.entity.AfterSaleMessage;
import com.yingjianxia.aftersales.entity.AfterSaleStatusLog;
import com.yingjianxia.aftersales.entity.ArbitrationRecord;
import com.yingjianxia.aftersales.enums.AfterSalesErrorCode;
import com.yingjianxia.aftersales.feign.EscrowFeignClient;
import com.yingjianxia.aftersales.mapper.AfterSaleMapper;
import com.yingjianxia.aftersales.mapper.AfterSaleMessageMapper;
import com.yingjianxia.aftersales.mapper.AfterSaleStatusLogMapper;
import com.yingjianxia.aftersales.mapper.ArbitrationRecordMapper;
import com.yingjianxia.aftersales.service.AfterSalesService;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后服务实现 — 状态机流转 + 状态日志 + escrow 退款触发 + 仲裁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSalesServiceImpl implements AfterSalesService {

    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleStatusLogMapper statusLogMapper;
    private final AfterSaleMessageMapper messageMapper;
    private final ArbitrationRecordMapper arbitrationMapper;
    private final EscrowFeignClient escrowFeignClient;

    /** 售后申请窗口期（天） */
    private static final int APPLY_WINDOW_DAYS = 7;
    /** 卖家自动处理期限（小时） */
    private static final int SELLER_AUTO_HANDLE_HOURS = 48;
    /** 内部调用密钥 */
    @Value("${yingjianxia.internal-secret}") private String internalSecret;

    /* ======================== 买家端 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyAfterSale(AfterSaleCreateReq req, Long buyerId) {
        // 1. 类型与明细校验
        if (req.getType() == null || req.getType() < 1 || req.getType() > 3) {
            throw new BusinessException(AfterSalesErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (req.getType() == AfterSale.TYPE_EXCHANGE && req.getOrderItemId() == null) {
            throw new BusinessException(AfterSalesErrorCode.ORDER_ITEM_REQUIRED);
        }
        // 2. 凭证图片数量
        if (req.getEvidenceImages() != null && req.getEvidenceImages().size() > 6) {
            throw new BusinessException(AfterSalesErrorCode.EVIDENCE_IMAGES_EXCEED);
        }
        // 3. 退款金额
        if (req.getRefundAmount() == null || req.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(AfterSalesErrorCode.REFUND_AMOUNT_INVALID);
        }
        // TODO: 调 order-service 查订单状态（须为已支付/已发货/已收货）+ 订单实付金额 ceiling + 收货时间7天窗口
        //   当前以本地简化校验：退款金额 > 0 已校验，真实 ceiling 需要 OrderFeignClient 合同确定后接入
        // 4. 重复申请校验（同一订单明细存在进行中的售后）
        Long inProgress;
        if (req.getOrderItemId() != null) {
            inProgress = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                    .eq(AfterSale::getOrderId, req.getOrderId())
                    .eq(AfterSale::getOrderItemId, req.getOrderItemId())
                    .in(AfterSale::getStatus,
                            AfterSale.STATUS_REVIEWING, AfterSale.STATUS_APPROVED,
                            AfterSale.STATUS_RETURN_PENDING, AfterSale.STATUS_REFUND_PENDING,
                            AfterSale.STATUS_ARBITRATING));
        } else {
            inProgress = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                    .eq(AfterSale::getOrderId, req.getOrderId())
                    .in(AfterSale::getStatus,
                            AfterSale.STATUS_REVIEWING, AfterSale.STATUS_APPROVED,
                            AfterSale.STATUS_RETURN_PENDING, AfterSale.STATUS_REFUND_PENDING,
                            AfterSale.STATUS_ARBITRATING));
        }
        if (inProgress != null && inProgress > 0) {
            throw new BusinessException(AfterSalesErrorCode.DUPLICATE_APPLY);
        }

        // 5. 落库
        LocalDateTime now = LocalDateTime.now();
        AfterSale a = new AfterSale();
        a.setAsNo(generateAsNo());
        a.setOrderId(req.getOrderId());
        a.setOrderItemId(req.getOrderItemId());
        a.setBuyerId(buyerId);
        a.setSellerId(req.getSellerId());
        a.setType(req.getType());
        a.setReason(req.getReason());
        a.setDescription(req.getDescription());
        a.setEvidenceImages(req.getEvidenceImages() == null ? null : JSONUtil.toJsonStr(req.getEvidenceImages()));
        a.setRefundAmount(req.getRefundAmount());
        a.setReturnShippingFee(BigDecimal.ZERO);
        a.setStatus(AfterSale.STATUS_REVIEWING);
        a.setArbitrationApplied(false);
        a.setDeadline(now.plusDays(APPLY_WINDOW_DAYS));
        a.setAutoProcessDeadline(now.plusHours(SELLER_AUTO_HANDLE_HOURS));
        afterSaleMapper.insert(a);

        // 6. 状态日志
        logStatusChange(a.getId(), null, AfterSale.STATUS_REVIEWING, buyerId,
                AfterSale.OPERATOR_BUYER, "买家发起售后申请");
        log.info("【售后申请】asNo={}, orderId={}, buyerId={}, type={}",
                a.getAsNo(), a.getOrderId(), buyerId, a.getType());
        return a.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAfterSale(Long afterSaleId, Long buyerId) {
        AfterSale a = requireAfterSale(afterSaleId);
        if (!a.getBuyerId().equals(buyerId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        if (a.getStatus() != AfterSale.STATUS_REVIEWING
                && a.getStatus() != AfterSale.STATUS_REFUSED) {
            throw new BusinessException(AfterSalesErrorCode.INVALID_STATUS_TRANSITION);
        }
        int from = a.getStatus();
        updateStatus(a, AfterSale.STATUS_CLOSED);
        logStatusChange(a.getId(), from, AfterSale.STATUS_CLOSED, buyerId,
                AfterSale.OPERATOR_BUYER, "买家取消售后");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillReturnShipment(ReturnShipmentReq req, Long buyerId) {
        AfterSale a = requireAfterSale(req.getAfterSaleId());
        if (!a.getBuyerId().equals(buyerId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        if (a.getStatus() != AfterSale.STATUS_RETURN_PENDING) {
            throw new BusinessException(AfterSalesErrorCode.INVALID_STATUS_TRANSITION);
        }
        LocalDateTime now = LocalDateTime.now();
        afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                .eq(AfterSale::getId, a.getId())
                .set(AfterSale::getReturnExpress, req.getReturnExpress())
                .set(AfterSale::getReturnTrackingNo, req.getReturnTrackingNo())
                .set(AfterSale::getUpdatedAt, now));
        log.info("【退货物流填写】asNo={}, express={}, trackingNo={}",
                a.getAsNo(), req.getReturnExpress(), req.getReturnTrackingNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyArbitration(Long afterSaleId, Long buyerId) {
        AfterSale a = requireAfterSale(afterSaleId);
        if (!a.getBuyerId().equals(buyerId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        // 仅在 拒绝/审核中 状态可申请仲裁
        if (a.getStatus() != AfterSale.STATUS_REFUSED
                && a.getStatus() != AfterSale.STATUS_REVIEWING) {
            throw new BusinessException(AfterSalesErrorCode.INVALID_STATUS_TRANSITION);
        }
        int from = a.getStatus();
        afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                .eq(AfterSale::getId, a.getId())
                .set(AfterSale::getArbitrationApplied, true)
                .set(AfterSale::getStatus, AfterSale.STATUS_ARBITRATING)
                .set(AfterSale::getUpdatedAt, LocalDateTime.now()));
        logStatusChange(a.getId(), from, AfterSale.STATUS_ARBITRATING, buyerId,
                AfterSale.OPERATOR_BUYER, "买家申请平台仲裁");
    }

    /* ======================== 卖家端 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerHandle(SellerHandleReq req, Long sellerId) {
        AfterSale a = requireAfterSale(req.getAfterSaleId());
        if (!a.getSellerId().equals(sellerId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        if (a.getStatus() != AfterSale.STATUS_REVIEWING) {
            throw new BusinessException(AfterSalesErrorCode.SELLER_ALREADY_HANDLE);
        }
        if (req.getAgree() == null) {
            throw new BusinessException(AfterSalesErrorCode.SELLER_RESPONSE_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();

        if (req.getAgree() == AfterSale.SELLER_AGREE) {
            // 同意：0 → 1已通过
            updateStatus(a, AfterSale.STATUS_APPROVED);
            logStatusChange(a.getId(), AfterSale.STATUS_REVIEWING, AfterSale.STATUS_APPROVED,
                    sellerId, AfterSale.OPERATOR_SELLER, "卖家同意售后");

            // 仅退款：1 → 3待退款 → 触发退款
            if (a.getType() == AfterSale.TYPE_REFUND_ONLY) {
                updateStatusTo(a, AfterSale.STATUS_REFUND_PENDING);
                logStatusChange(a.getId(), AfterSale.STATUS_APPROVED, AfterSale.STATUS_REFUND_PENDING,
                        sellerId, AfterSale.OPERATOR_SELLER, "仅退款直接进入待退款");
                triggerRefund(a, sellerId, AfterSale.OPERATOR_SELLER, "仅退款");
            } else {
                // 退货退款 / 换货：1 → 2待退货
                afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                        .eq(AfterSale::getId, a.getId())
                        .set(AfterSale::getSellerResponse, AfterSale.SELLER_AGREE)
                        .set(AfterSale::getSellerRemark, req.getRemark())
                        .set(req.getShippingFeeBearer() != null, AfterSale::getShippingFeeBearer, req.getShippingFeeBearer())
                        .set(StrUtil.isNotBlank(req.getReturnAddress()), AfterSale::getReturnAddress, req.getReturnAddress())
                        .set(AfterSale::getStatus, AfterSale.STATUS_RETURN_PENDING)
                        .set(AfterSale::getUpdatedAt, now));
                logStatusChange(a.getId(), AfterSale.STATUS_APPROVED, AfterSale.STATUS_RETURN_PENDING,
                        sellerId, AfterSale.OPERATOR_SELLER, "卖家同意退货，等待买家回退商品");
            }
        } else {
            // 拒绝：0 → 5已拒绝
            afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                    .eq(AfterSale::getId, a.getId())
                    .set(AfterSale::getSellerResponse, AfterSale.SELLER_REFUSE)
                    .set(AfterSale::getSellerRefuseReason, req.getRefuseReason())
                    .set(AfterSale::getSellerRemark, req.getRemark())
                    .set(AfterSale::getStatus, AfterSale.STATUS_REFUSED)
                    .set(AfterSale::getUpdatedAt, now));
            logStatusChange(a.getId(), AfterSale.STATUS_REVIEWING, AfterSale.STATUS_REFUSED,
                    sellerId, AfterSale.OPERATOR_SELLER, "卖家拒绝售后：" + req.getRefuseReason());
        }
    }

    @Override
    public void confirmReturnReceived(Long afterSaleId, Long sellerId) {
        AfterSale a = requireAfterSale(afterSaleId);
        if (!a.getSellerId().equals(sellerId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        if (a.getStatus() != AfterSale.STATUS_RETURN_PENDING) {
            throw new BusinessException(AfterSalesErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (StrUtil.hasBlank(a.getReturnExpress(), a.getReturnTrackingNo())) {
            throw new BusinessException(AfterSalesErrorCode.RETURN_SHIPMENT_NOT_FILLED);
        }
        // 2待退货 → 3待退款（单独事务）
        updateStatusTo(a, AfterSale.STATUS_REFUND_PENDING);
        logStatusChange(a.getId(), AfterSale.STATUS_RETURN_PENDING, AfterSale.STATUS_REFUND_PENDING,
                sellerId, AfterSale.OPERATOR_SELLER, "卖家确认收到退货");
        // 触发退款
        triggerRefund(a, sellerId, AfterSale.OPERATOR_SELLER, "退货退款");
    }

    /* ======================== 客服端 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arbitrate(ArbitrationReq req, Long arbitratorId, String arbitratorName) {
        AfterSale a = requireAfterSale(req.getAfterSaleId());
        if (a.getStatus() != AfterSale.STATUS_ARBITRATING) {
            throw new BusinessException(AfterSalesErrorCode.INVALID_STATUS_TRANSITION);
        }
        // 仲裁记录唯一性
        Long exist = arbitrationMapper.selectCount(new LambdaQueryWrapper<ArbitrationRecord>()
                .eq(ArbitrationRecord::getAfterSaleId, req.getAfterSaleId()));
        if (exist != null && exist > 0) {
            throw new BusinessException(AfterSalesErrorCode.ARBITRATION_ALREADY_EXISTS);
        }
        if (req.getResult() == null || req.getResult() < 1 || req.getResult() > 3) {
            throw new BusinessException(AfterSalesErrorCode.ARBITRATION_RESULT_INVALID);
        }
        if (req.getResult() == ArbitrationRecord.RESULT_PARTIAL_REFUND
                && (req.getRefundAmount() == null || req.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(AfterSalesErrorCode.ARBITRATION_REFUND_INVALID);
        }

        // 写仲裁记录
        ArbitrationRecord ar = new ArbitrationRecord();
        ar.setAfterSaleId(req.getAfterSaleId());
        ar.setArbitratorId(arbitratorId);
        ar.setArbitratorName(arbitratorName);
        ar.setResult(req.getResult());
        ar.setRefundAmount(req.getRefundAmount());
        ar.setReason(req.getReason());
        ar.setEvidenceSummary(req.getEvidenceSummary());
        arbitrationMapper.insert(ar);

        int from = a.getStatus();
        switch (req.getResult()) {
            case ArbitrationRecord.RESULT_SUPPORT_BUYER:
                // 支持买家：全额退款 6 → 3待退款 → 触发退款 → 4已完成
                updateStatusTo(a, AfterSale.STATUS_REFUND_PENDING);
                logStatusChange(a.getId(), from, AfterSale.STATUS_REFUND_PENDING,
                        arbitratorId, AfterSale.OPERATOR_ARBITRATOR, "仲裁支持买家，全额退款");
                triggerRefund(a, arbitratorId, AfterSale.OPERATOR_ARBITRATOR, "仲裁全额退款");
                break;
            case ArbitrationRecord.RESULT_PARTIAL_REFUND:
                // 部分退款：覆盖退款金额 6 → 3待退款 → 触发部分退款 → 4已完成
                afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                        .eq(AfterSale::getId, a.getId())
                        .set(AfterSale::getRefundAmount, req.getRefundAmount())
                        .set(AfterSale::getStatus, AfterSale.STATUS_REFUND_PENDING)
                        .set(AfterSale::getUpdatedAt, LocalDateTime.now()));
                logStatusChange(a.getId(), from, AfterSale.STATUS_REFUND_PENDING,
                        arbitratorId, AfterSale.OPERATOR_ARBITRATOR, "仲裁部分退款，金额=" + req.getRefundAmount());
                triggerRefund(a, arbitratorId, AfterSale.OPERATOR_ARBITRATOR, "仲裁部分退款");
                break;
            case ArbitrationRecord.RESULT_SUPPORT_SELLER:
                // 支持卖家：6 → 7已关闭
                updateStatusTo(a, AfterSale.STATUS_CLOSED);
                logStatusChange(a.getId(), from, AfterSale.STATUS_CLOSED,
                        arbitratorId, AfterSale.OPERATOR_ARBITRATOR, "仲裁支持卖家，售后关闭");
                break;
            default:
                break;
        }
    }

    /* ======================== 沟通 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(AsMessageReq req, Long senderId, Integer senderType) {
        AfterSale a = requireAfterSale(req.getAfterSaleId());
        // 权限：买家/卖家只能对自己的售后单发消息
        if (senderType == AfterSale.OPERATOR_BUYER && !a.getBuyerId().equals(senderId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        if (senderType == AfterSale.OPERATOR_SELLER && !a.getSellerId().equals(senderId)) {
            throw new BusinessException(AfterSalesErrorCode.NO_PERMISSION);
        }
        if (StrUtil.isBlank(req.getContent())
                && (req.getImages() == null || req.getImages().isEmpty())) {
            throw new BusinessException(AfterSalesErrorCode.MESSAGE_EMPTY);
        }
        AfterSaleMessage m = new AfterSaleMessage();
        m.setAfterSaleId(req.getAfterSaleId());
        m.setSenderId(senderId);
        m.setSenderType(senderType);
        m.setContent(req.getContent());
        m.setImages(req.getImages() == null ? null : JSONUtil.toJsonStr(req.getImages()));
        messageMapper.insert(m);
    }

    @Override
    public List<AfterSaleMessage> listMessages(Long afterSaleId) {
        return messageMapper.selectList(new LambdaQueryWrapper<AfterSaleMessage>()
                .eq(AfterSaleMessage::getAfterSaleId, afterSaleId)
                .orderByAsc(AfterSaleMessage::getCreatedAt));
    }

    /* ======================== 查询 ======================== */

    @Override
    public AfterSale getDetail(Long afterSaleId) {
        return requireAfterSale(afterSaleId);
    }

    @Override
    public AfterSale getDetailWithArbitration(Long afterSaleId) {
        AfterSale a = requireAfterSale(afterSaleId);
        return a;
    }

    @Override
    public ArbitrationRecord getArbitration(Long afterSaleId) {
        return arbitrationMapper.selectOne(new LambdaQueryWrapper<ArbitrationRecord>()
                .eq(ArbitrationRecord::getAfterSaleId, afterSaleId));
    }

    @Override
    public PageResult<AfterSale> pageQuery(AfterSaleQueryReq req, Long operatorId) {
        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<>();
        if (req.getOrderId() != null) qw.eq(AfterSale::getOrderId, req.getOrderId());
        if (req.getStatus() != null) qw.eq(AfterSale::getStatus, req.getStatus());
        if (req.getType() != null) qw.eq(AfterSale::getType, req.getType());
        if (StrUtil.isNotBlank(req.getAsNo())) qw.like(AfterSale::getAsNo, req.getAsNo());
        // 角色过滤
        Integer role = req.getRole();
        if (role != null && role == AfterSale.OPERATOR_BUYER) {
            qw.eq(AfterSale::getBuyerId, operatorId);
        } else if (role != null && role == AfterSale.OPERATOR_SELLER) {
            qw.eq(AfterSale::getSellerId, operatorId);
        }
        // 客服（role=3）不过滤
        qw.orderByDesc(AfterSale::getCreatedAt);

        Page<AfterSale> page = new Page<>(req.getPageNum(), req.getPageSize());
        Page<AfterSale> result = afterSaleMapper.selectPage(page, qw);
        return PageResult.of(req.getPageNum(), req.getPageSize(), result.getTotal(), result.getRecords());
    }

    /* ======================== 定时任务 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoAgreeTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<AfterSale> candidates = afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getStatus, AfterSale.STATUS_REVIEWING)
                .lt(AfterSale::getAutoProcessDeadline, now));
        if (candidates.isEmpty()) return;
        for (AfterSale a : candidates) {
            try {
                // 超时自动同意，按类型走同意流程
                updateStatusTo(a, AfterSale.STATUS_APPROVED);
                logStatusChange(a.getId(), AfterSale.STATUS_REVIEWING, AfterSale.STATUS_APPROVED,
                        0L, AfterSale.OPERATOR_SYSTEM, "卖家超时未处理，系统自动同意");
                if (a.getType() == AfterSale.TYPE_REFUND_ONLY) {
                    updateStatusTo(a, AfterSale.STATUS_REFUND_PENDING);
                    logStatusChange(a.getId(), AfterSale.STATUS_APPROVED, AfterSale.STATUS_REFUND_PENDING,
                            0L, AfterSale.OPERATOR_SYSTEM, "仅退款自动进入待退款");
                    triggerRefund(a, 0L, AfterSale.OPERATOR_SYSTEM, "超时自动同意-仅退款");
                } else {
                    afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                            .eq(AfterSale::getId, a.getId())
                            .set(AfterSale::getSellerResponse, AfterSale.SELLER_AGREE)
                            .set(AfterSale::getStatus, AfterSale.STATUS_RETURN_PENDING)
                            .set(AfterSale::getUpdatedAt, now));
                    logStatusChange(a.getId(), AfterSale.STATUS_APPROVED, AfterSale.STATUS_RETURN_PENDING,
                            0L, AfterSale.OPERATOR_SYSTEM, "超时自动同意退货，等待买家回退");
                }
            } catch (Exception e) {
                log.error("【超时自动同意】处理失败 asNo={} err={}", a.getAsNo(), e.getMessage(), e);
            }
        }
        log.info("【超时自动同意】共处理 {} 条售后单", candidates.size());
    }

    /* ======================== 内部工具 ======================== */

    private AfterSale requireAfterSale(Long id) {
        AfterSale a = afterSaleMapper.selectById(id);
        if (a == null) throw new BusinessException(AfterSalesErrorCode.AFTER_SALE_NOT_FOUND);
        return a;
    }

    private void updateStatus(AfterSale a, int toStatus) {
        updateStatusTo(a, toStatus);
    }

    private void updateStatusTo(AfterSale a, int toStatus) {
        afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                .eq(AfterSale::getId, a.getId())
                .set(AfterSale::getStatus, toStatus)
                .set(AfterSale::getUpdatedAt, LocalDateTime.now()));
        a.setStatus(toStatus);
    }

    private void logStatusChange(Long afterSaleId, Integer from, int to,
                                 Long operatorId, int operatorType, String remark) {
        AfterSaleStatusLog statusLog = new AfterSaleStatusLog();
        statusLog.setAfterSaleId(afterSaleId);
        statusLog.setFromStatus(from);
        statusLog.setToStatus(to);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorType(operatorType);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
    }

    /**
     * 触发退款（调 escrow-service）— 非事务内执行，成功后置 4已完成
     */
    private void triggerRefund(AfterSale a, Long operatorId, int operatorType, String reason) {
        RefundTriggerReq req = new RefundTriggerReq();
        req.setAfterSaleId(a.getId());
        req.setOrderId(a.getOrderId());
        req.setBuyerId(a.getBuyerId());
        req.setSellerId(a.getSellerId());
        req.setRefundAmount(a.getRefundAmount());
        req.setReason(reason);

        ApiResponse<String> resp;
        try {
            resp = escrowFeignClient.triggerRefund(req, internalSecret);
        } catch (Exception e) {
            log.error("【触发退款】escrow 调用异常 asNo={} err={}", a.getAsNo(), e.getMessage(), e);
            throw new BusinessException(AfterSalesErrorCode.ESCROW_REFUND_FAIL);
        }
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            throw new BusinessException(AfterSalesErrorCode.ESCROW_REFUND_FAIL);
        }
        // 退款成功 → 4已完成
        LocalDateTime now = LocalDateTime.now();
        afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                .eq(AfterSale::getId, a.getId())
                .set(AfterSale::getStatus, AfterSale.STATUS_COMPLETED)
                .set(AfterSale::getRefundTxNo, resp.getData())
                .set(AfterSale::getRefundAt, now)
                .set(AfterSale::getCompletedAt, now)
                .set(AfterSale::getUpdatedAt, now));
        logStatusChange(a.getId(), AfterSale.STATUS_REFUND_PENDING, AfterSale.STATUS_COMPLETED,
                operatorId, operatorType, "退款完成，流水号=" + resp.getData());
        log.info("【退款完成】asNo={}, refundTxNo={}", a.getAsNo(), resp.getData());
    }

    /**
     * 生成售后单号：AS + yyyyMMddHHmmss + 4位随机
     */
    private String generateAsNo() {
        return "AS" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss")
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}
