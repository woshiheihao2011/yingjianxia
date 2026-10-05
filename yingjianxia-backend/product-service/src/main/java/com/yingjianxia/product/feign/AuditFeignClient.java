package com.yingjianxia.product.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 审核服务 Feign Client — 商品提交审核时通知 audit-service 创建审核记录
 *
 * @author 硬件侠后端团队
 */
@FeignClient(name = "audit-service", contextId = "productAuditSubmit", fallback = AuditFeignFallback.class)
public interface AuditFeignClient {

    /**
     * 提交审核任务
     *
     * @param targetType 目标类型：1=商品
     * @param targetId  目标ID（商品ID）
     * @return 审核记录ID
     */
    @PostMapping("/api/v1/audit/submit")
    ApiResponse<Long> submitAudit(@RequestParam("targetType") Integer targetType,
                                  @RequestParam("targetId") Long targetId,
                                  @RequestBody(required = false) Map<String, Object> body);
}
