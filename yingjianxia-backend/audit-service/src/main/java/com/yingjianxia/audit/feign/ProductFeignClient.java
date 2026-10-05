package com.yingjianxia.audit.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 商品服务 Feign Client — 审核通过/拒绝后通知商品状态变更
 *
 * @author 硬件侠后端团队
 */
@FeignClient(name = "product-service", contextId = "productAuditClient", fallback = ProductFeignFallback.class)
public interface ProductFeignClient {

    /**
     * 审核通过 → 商品上架（REVIEWING → ON_SALE）
     */
    @PostMapping("/api/v1/product/internal/{productId}/audit-pass")
    ApiResponse<Void> auditPass(@PathVariable("productId") Long productId,
                               @RequestHeader("X-Internal-Secret") String secret,
                               @RequestHeader("X-Auditor-Id") Long auditorId);

    /**
     * 审核拒绝 → 商品状态变为审核拒绝
     */
    @PostMapping("/api/v1/product/internal/{productId}/audit-reject")
    ApiResponse<Void> auditReject(@PathVariable("productId") Long productId,
                                 @RequestHeader("X-Internal-Secret") String secret,
                                 @RequestHeader("X-Auditor-Id") Long auditorId);
}
