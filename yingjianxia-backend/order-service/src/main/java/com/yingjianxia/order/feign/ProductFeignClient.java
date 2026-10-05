package com.yingjianxia.order.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Product-service Feign 客户端（内部调用）
 * <p>
 * 用于订单创建时获取商品信息（标题、价格、卖家ID等）
 *
 * @author 硬件侠后端团队
 */
@FeignClient(name = "product-service", contextId = "productFeignClient")
public interface ProductFeignClient {

    /**
     * 获取商品详情（内部端点，不过滤状态）
     */
    @GetMapping("/api/v1/product/internal/{productId}")
    ApiResponse<Map<String, Object>> getProductDetail(@PathVariable("productId") Long productId);
}
