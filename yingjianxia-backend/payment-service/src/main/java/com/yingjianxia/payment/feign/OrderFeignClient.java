package com.yingjianxia.payment.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 订单服务 Feign 客户端（payment-service → order-service）
 * <p>
 * 用于获取订单金额，避免前端重复传 amount
 */
@FeignClient(name = "order-service", contextId = "paymentOrderFeignClient")
public interface OrderFeignClient {

    /**
     * 获取订单详情（内部端点，不过滤状态）
     */
    @GetMapping("/api/v1/orders/{orderId}")
    ApiResponse<Map<String, Object>> getOrderDetail(@PathVariable("orderId") Long orderId);
}
