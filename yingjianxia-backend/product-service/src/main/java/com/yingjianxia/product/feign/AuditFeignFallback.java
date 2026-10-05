package com.yingjianxia.product.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AuditFeignClient 降级处理
 * 审核服务不可用时，不阻塞商品提交审核流程（最终一致性由补偿机制保证）
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
public class AuditFeignFallback implements AuditFeignClient {

    @Override
    public ApiResponse<Long> submitAudit(Integer targetType, Long targetId, Map<String, Object> body) {
        log.warn("[Sentinel 降级] audit-service#submitAudit 不可用, targetType={}, targetId={}", targetType, targetId);
        return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE.getCode(), "审核服务暂不可用，审核记录将延迟创建");
    }
}
