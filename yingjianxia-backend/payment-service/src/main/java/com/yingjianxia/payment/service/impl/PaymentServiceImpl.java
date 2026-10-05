package com.yingjianxia.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.payment.dto.CreatePaymentReq;
import com.yingjianxia.payment.dto.PaymentCallbackReq;
import com.yingjianxia.payment.dto.RefundCallbackReq;
import com.yingjianxia.payment.entity.PaymentOrder;
import com.yingjianxia.payment.enums.PaymentErrorCode;
import com.yingjianxia.payment.feign.OrderFeignClient;
import com.yingjianxia.payment.mapper.PaymentOrderMapper;
import com.yingjianxia.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现
 * <p>
 * 三大资金安全基石：
 * <ol>
 *   <li>回调验签 — 防伪造回调</li>
 *   <li>幂等处理 — 防重复回调</li>
 *   <li>主动查询兜底 — 防丢回调</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final OrderFeignClient orderFeignClient;

    @Value("${yingjianxia.payment.wechat.api-key}")
    private String wxApiKey;

    @Value("${yingjianxia.payment.alipay.private-key}")
    private String aliPrivateKey;

    /** 回调幂等 key 前缀，TTL 7 天兜底 */
    private static final String CALLBACK_IDEMPOTENT_PREFIX = "payment:callback:";
    private static final long CALLBACK_TTL_DAYS = 7L;
    /** 支付单默认有效期（分钟） */
    private static final int DEFAULT_EXPIRE_MINUTES = 30;

    /* ======================== 创建支付单 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPayment(CreatePaymentReq req) {
        // 1. 渠道校验
        String channel = req.getChannel();
        if (!PaymentOrder.CHANNEL_WECHAT.equals(channel)
                && !PaymentOrder.CHANNEL_ALIPAY.equals(channel)
                && !PaymentOrder.CHANNEL_WALLET.equals(channel)) {
            throw new BusinessException(PaymentErrorCode.CHANNEL_NOT_SUPPORTED);
        }

        // 2. 幂等校验：同 idempotencyKey 不重复创建
        if (StrUtil.isNotBlank(req.getIdempotencyKey())) {
            PaymentOrder exist = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getIdempotencyKey, req.getIdempotencyKey()));
            if (exist != null) {
                return exist; // 返回已有支付单
            }
        }

        // 3. 构建支付单
        Long userId = UserContext.requiredUserId();
        PaymentOrder order = new PaymentOrder();
        order.setPaymentNo(generatePaymentNo());
        order.setOrderId(req.getOrderId());
        order.setUserId(userId);
        // amount 可选：前端不传时从 order-service 获取订单实际金额
        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            try {
                ApiResponse<Map<String, Object>> orderResp = orderFeignClient.getOrderDetail(req.getOrderId());
                if (orderResp != null && orderResp.getData() != null) {
                    Object payAmt = orderResp.getData().get("payableAmount");
                    if (payAmt != null) {
                        amount = new BigDecimal(payAmt.toString());
                    }
                }
            } catch (Exception e) {
                log.warn("【支付创建】Feign 获取订单金额失败, orderId={}, err={}", req.getOrderId(), e.getMessage());
            }
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(6001, "支付金额无效：无法从订单获取金额");
        }
        order.setAmount(amount);
        order.setChannel(channel);
        order.setStatus(PaymentOrder.STATUS_PENDING);
        order.setExpireAt(LocalDateTime.now().plusMinutes(DEFAULT_EXPIRE_MINUTES));
        order.setIdempotencyKey(StrUtil.isBlank(req.getIdempotencyKey()) ? IdUtil.fastSimpleUUID() : req.getIdempotencyKey());
        paymentOrderMapper.insert(order);

        // 4. 调用第三方统一下单 API（占位）
        String channelOrderNo = callUnifiedOrder(order);
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, order.getId())
                .set(PaymentOrder::getChannelOrderNo, channelOrderNo)
                .set(PaymentOrder::getUpdatedAt, LocalDateTime.now()));

        order.setChannelOrderNo(channelOrderNo);
        log.info("【支付单创建】paymentNo={}, orderId={}, channel={}, amount={}", order.getPaymentNo(), order.getOrderId(), channel, req.getAmount());
        return order;
    }

    /* ======================== 支付回调处理 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(PaymentCallbackReq req) {
        // 1. 查支付单
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, req.getPaymentNo()));
        if (order == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        // 2. 幂等：已支付直接返回成功（防重复回调）
        if (order.getStatus() == PaymentOrder.STATUS_PAID) {
            log.info("【支付回调幂等】paymentNo={} 已支付，跳过", req.getPaymentNo());
            return;
        }
        if (order.getStatus() == PaymentOrder.STATUS_CLOSED || order.getStatus() == PaymentOrder.STATUS_REFUNDED) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_STATUS_INVALID);
        }

        // 3. Redis 幂等锁（双重防护：DB 状态 + Redis SETNX）
        String idempotentKey = CALLBACK_IDEMPOTENT_PREFIX + "pay:" + req.getPaymentNo();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", CALLBACK_TTL_DAYS, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("【支付回调Redis幂等拦截】paymentNo={}", req.getPaymentNo());
            throw new BusinessException(PaymentErrorCode.CALLBACK_DUPLICATE);
        }

        // 4. 验签
        verifyCallbackSign(order.getChannel(), req.getSign(), req.getRawData(), req.getTimestamp());

        // 5. 金额校验
        if (order.getAmount().compareTo(req.getAmount()) != 0) {
            log.error("【回调金额不匹配】paymentNo={}, db={}, callback={}", req.getPaymentNo(), order.getAmount(), req.getAmount());
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 6. 更新支付单状态 → 已支付
        LocalDateTime now = LocalDateTime.now();
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, order.getId())
                .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_PENDING) // 乐观条件
                .set(PaymentOrder::getStatus, PaymentOrder.STATUS_PAID)
                .set(PaymentOrder::getChannelOrderNo, req.getChannelOrderNo())
                .set(PaymentOrder::getPaidAt, now)
                .set(PaymentOrder::getCallbackRaw, req.getRawData())
                .set(PaymentOrder::getUpdatedAt, now));

        // 7. 通知 escrow-service 入账（Outbox 事件占位）
        // TODO: OutboxPattern → "payment.paid" 事件 → escrow-service 担保冻结/钱包入账
        log.info("【支付回调成功】paymentNo={}, orderId={}, channelOrderNo={}", req.getPaymentNo(), order.getOrderId(), req.getChannelOrderNo());
    }

    /* ======================== 退款回调处理 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundCallback(RefundCallbackReq req) {
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, req.getPaymentNo()));
        if (order == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        // 幂等：已退款直接返回
        if (order.getStatus() == PaymentOrder.STATUS_REFUNDED) {
            log.info("【退款回调幂等】paymentNo={} 已退款，跳过", req.getPaymentNo());
            return;
        }

        // Redis 幂等锁
        String idempotentKey = CALLBACK_IDEMPOTENT_PREFIX + "refund:" + req.getChannelRefundNo();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", CALLBACK_TTL_DAYS, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException(PaymentErrorCode.REFUND_CALLBACK_DUPLICATE);
        }

        // 验签
        verifyCallbackSign(order.getChannel(), req.getSign(), req.getRawData(), req.getTimestamp());

        // 退款金额校验
        if (req.getRefundAmount().compareTo(order.getAmount()) > 0) {
            throw new BusinessException(PaymentErrorCode.REFUND_AMOUNT_EXCEED);
        }

        // 更新状态 → 已退款
        LocalDateTime now = LocalDateTime.now();
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, order.getId())
                .in(PaymentOrder::getStatus, PaymentOrder.STATUS_PAID) // 仅已支付可退
                .set(PaymentOrder::getStatus, PaymentOrder.STATUS_REFUNDED)
                .set(PaymentOrder::getCallbackRaw, req.getRawData())
                .set(PaymentOrder::getUpdatedAt, now));

        // 通知 escrow-service 退款入账
        // TODO: OutboxPattern → "payment.refunded" 事件 → escrow-service 解冻退款
        log.info("【退款回调成功】paymentNo={}, refundNo={}, amount={}", req.getPaymentNo(), req.getChannelRefundNo(), req.getRefundAmount());
    }

    /* ======================== 查询支付状态 ======================== */

    @Override
    public PaymentOrder queryPaymentStatus(String paymentNo) {
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, paymentNo));
        if (order == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        // 主动查询兜底：待支付且未过期时，可触发渠道主动查询（占位）
        if (order.getStatus() == PaymentOrder.STATUS_PENDING && order.getExpireAt() != null
                && order.getExpireAt().isAfter(LocalDateTime.now())) {
            // TODO: 调用微信/支付宝订单查询 API 兜底
            log.debug("【主动查询兜底占位】paymentNo={}", paymentNo);
        }
        return order;
    }

    /* ======================== 超时关单 ======================== */

    @Override
    @Scheduled(cron = "0 */5 * * * ?") // 每5分钟扫描
    public int closeExpiredOrders() {
        List<PaymentOrder> expired = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_PENDING)
                .lt(PaymentOrder::getExpireAt, LocalDateTime.now()));

        int closed = 0;
        for (PaymentOrder order : expired) {
            int rows = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getId, order.getId())
                    .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_PENDING) // 乐观条件防并发
                    .set(PaymentOrder::getStatus, PaymentOrder.STATUS_CLOSED)
                    .set(PaymentOrder::getUpdatedAt, LocalDateTime.now()));
            if (rows > 0) {
                closed++;
                log.info("【超时关单】paymentNo={}, expireAt={}", order.getPaymentNo(), order.getExpireAt());
            }
        }
        if (closed > 0) {
            log.info("【超时关单批次】共关闭 {} 笔过期支付单", closed);
        }
        return closed;
    }

    /* ======================== 内部工具 ======================== */

    /**
     * 生成支付单号：PAY + yyyyMMddHHmmss + 6位随机
     */
    private String generatePaymentNo() {
        String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String rand = IdUtil.fastSimpleUUID().substring(0, 6);
        return "PAY" + ts + rand;
    }

    /**
     * 调用第三方统一下单 API（开发期占位）
     * <p>
     * 生产环境：
     * - 微信：POST https://api.mch.weixin.qq.com/pay/unifiedorder，返回 prepay_id
     * - 支付宝：alipay.trade.precreate 或 alipay.trade.create，返回 qr_code / trade_no
     */
    private String callUnifiedOrder(PaymentOrder order) {
        // 开发占位：返回一个伪渠道订单号
        String mockChannelNo = "MOCK_" + order.getChannel().toUpperCase() + "_" + IdUtil.fastSimpleUUID().substring(0, 12);
        log.debug("【统一下单占位】paymentNo={}, channel={}, mockChannelNo={}", order.getPaymentNo(), order.getChannel(), mockChannelNo);
        return mockChannelNo;
    }

    /**
     * 回调验签
     * <p>
     * - 微信：MD5(排序参数串 + &key=API_KEY)
     * - 支付宝：RSA2 验签（公钥验签）
     * <p>
     * 开发期占位：使用 HMAC-MD5 模拟验签
     */
    private void verifyCallbackSign(String channel, String sign, String rawData, Long timestamp) {
        // 时间戳防重放（5 分钟窗口）
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
        if (Math.abs(now - timestamp) > 300) {
            log.warn("【回调时间戳越界】ts={}, now={}", timestamp, now);
            throw new BusinessException(PaymentErrorCode.CALLBACK_SIGN_VERIFY_FAIL);
        }

        String expectedSign;
        if (PaymentOrder.CHANNEL_WECHAT.equals(channel)) {
            // 微信验签占位：MD5(rawData + apiKey)
            expectedSign = DigestUtil.md5Hex(rawData + wxApiKey);
        } else if (PaymentOrder.CHANNEL_ALIPAY.equals(channel)) {
            // 支付宝验签占位：MD5(rawData + privateKey)
            expectedSign = DigestUtil.md5Hex(rawData + aliPrivateKey);
        } else {
            throw new BusinessException(PaymentErrorCode.CHANNEL_NOT_SUPPORTED);
        }

        if (!expectedSign.equalsIgnoreCase(sign)) {
            log.error("【验签失败】channel={}, expected={}, actual={}", channel, expectedSign, sign);
            throw new BusinessException(PaymentErrorCode.CALLBACK_SIGN_VERIFY_FAIL);
        }
    }
}
