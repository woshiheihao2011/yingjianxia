package com.yingjianxia.audit.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ProductFeignClient 降级处理
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
public class ProductFeignFallback implements ProductFeignClient {

    @Override
    public ApiResponse<Void> auditPass(Long productId, String secret, Long auditorId) {
        log.warn("[Sentinel 降级] product-service#auditPass, productId={}, auditorId={}", productId, auditorId);
        return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE, "商品服务暂不可用，审核通过通知失败");
    }

    @Override
    public ApiResponse<Void> auditReject(Long productId, String secret, Long auditorId) {
        log.warn("[Sentinel 降级] product-service#auditReject, productId={}, auditorId={}", productId, auditorId);
        return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE, "商品服务暂不可用，审核拒绝通知失败");
    }
}
