package com.yingjianxia.product.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 店铺服务 Feign Client — 商品创建/详情时按 sellerId 查店铺信息（bug-20260908170645）
 * 复用 user-service 已有的公开端点 GET /api/v1/shop/by-seller/{sellerId}（已加入网关白名单）
 *
 * @author 硬件侠后端团队
 */
@FeignClient(name = "user-service", contextId = "shopFeignClient", fallback = ShopFeignFallback.class)
public interface ShopFeignClient {

    /**
     * 按卖家ID查询店铺信息
     *
     * @param sellerId 卖家用户ID
     * @return 店铺信息（无店铺返回 null）
     */
    @GetMapping("/api/v1/shop/by-seller/{sellerId}")
    ApiResponse<ShopFeignResp> getBySeller(@PathVariable("sellerId") Long sellerId);
}
