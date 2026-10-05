package com.yingjianxia.evaluation.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 订单服务 Feign 客户端（evaluation-service → order-service）
 * <p>
 * 用于获取订单关联的商品ID和卖家ID
 */
@FeignClient(name = "order-service", contextId = "evaluationOrderFeignClient")
public interface OrderFeignClient {

    /**
     * 获取订单详情
     */
    @GetMapping("/api/v1/orders/{orderId}")
    ApiResponse<Map<String, Object>> getOrderDetail(@PathVariable("orderId") Long orderId);
}
