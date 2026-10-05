package com.yingjianxia.payment.service;

import com.yingjianxia.payment.dto.CreatePaymentReq;
import com.yingjianxia.payment.dto.PaymentCallbackReq;
import com.yingjianxia.payment.dto.RefundCallbackReq;
import com.yingjianxia.payment.entity.PaymentOrder;

/**
 * 支付服务接口
 * <p>
 * 核心能力：创建支付单（统一下单占位）、回调验签+幂等、查询状态、退款回调、超时关单。
 *
 * @author 硬件侠后端团队
 */
public interface PaymentService {

    /**
     * 创建支付单（调用微信/支付宝统一下单API占位）
     *
     * @param req  创建支付请求
     * @return 支付单（含第三方预下单信息占位）
     */
    PaymentOrder createPayment(CreatePaymentReq req);

    /**
     * 支付回调处理：验签 → 更新状态 → 通知 escrow-service 入账 → 幂等
     *
     * @param req 回调请求
     */
    void handlePaymentCallback(PaymentCallbackReq req);

    /**
     * 退款回调处理：验签 → 标记已退款 → 通知 escrow-service 退款入账
     *
     * @param req 退款回调请求
     */
    void handleRefundCallback(RefundCallbackReq req);

    /**
     * 查询支付状态（优先读库，可触发主动查询渠道兜底）
     *
     * @param paymentNo 支付单号
     * @return 支付单
     */
    PaymentOrder queryPaymentStatus(String paymentNo);

    /**
     * 超时关单（定时任务调用）：将过期待支付单批量关闭
     *
     * @return 关闭数量
     */
    int closeExpiredOrders();
}
