package com.yingjianxia.payment.controller;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.payment.dto.CreatePaymentReq;
import com.yingjianxia.payment.dto.PaymentCallbackReq;
import com.yingjianxia.payment.dto.RefundCallbackReq;
import com.yingjianxia.payment.entity.PaymentOrder;
import com.yingjianxia.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

/**
 * 支付服务 Controller
 * <p>
 * 入口：
 * <ol>
 *   <li>创建支付单（买家端，下单后发起支付）</li>
 *   <li>微信回调 /wechat/notify（第三方异步通知）</li>
 *   <li>支付宝回调 /alipay/notify（第三方异步通知）</li>
 *   <li>查询支付状态（买家端轮询）</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Tag(name = "支付服务", description = "微信/支付宝统一下单、回调验签+幂等、查询状态、退款回调")
@RestController
@RequestMapping({"/api/v1/payment", "/api/v1/payments"})
@RequiredArgsConstructor
public class PaymentController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final PaymentService service;

    /* ========== 买家端 ========== */

    @Operation(summary = "[买家] 创建支付单（发起微信/支付宝支付）")
    @PostMapping({"", "/create"})
    public ApiResponse<PaymentOrder> create(@Valid @RequestBody CreatePaymentReq req) {
        return ApiResponse.success(service.createPayment(req));
    }

    @Operation(summary = "[买家] 查询支付状态（轮询）")
    @GetMapping({"/{paymentNo}/status", "/{paymentNo}"})
    public ApiResponse<PaymentOrder> status(@PathVariable String paymentNo) {
        return ApiResponse.success(service.queryPaymentStatus(paymentNo));
    }

    /* ========== 第三方回调（公开，由渠道直接调用） ========== */

    @Operation(summary = "微信支付回调")
    @PostMapping("/wechat/notify")
    public ApiResponse<Void> wechatNotify(@Valid @RequestBody PaymentCallbackReq req) {
        req.setChannel("wechat");
        service.handlePaymentCallback(req);
        return ApiResponse.success();
    }

    @Operation(summary = "支付宝支付回调")
    @PostMapping("/alipay/notify")
    public ApiResponse<Void> alipayNotify(@Valid @RequestBody PaymentCallbackReq req) {
        req.setChannel("alipay");
        service.handlePaymentCallback(req);
        return ApiResponse.success();
    }

    @Operation(summary = "退款回调（微信/支付宝通用）")
    @PostMapping("/refund/notify")
    public ApiResponse<Void> refundNotify(@Valid @RequestBody RefundCallbackReq req) {
        service.handleRefundCallback(req);
        return ApiResponse.success();
    }

    /* ========== [内部] 其他服务调用 ========== */

    @Operation(summary = "[内部] 查询支付单状态", hidden = true)
    @GetMapping("/internal/{paymentNo}")
    public ApiResponse<PaymentOrder> internalStatus(@PathVariable String paymentNo,
                                                      @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        try {
            return ApiResponse.success(service.queryPaymentStatus(paymentNo));
        } catch (Exception e) {
            return ApiResponse.success(null);
        }
    }
}
