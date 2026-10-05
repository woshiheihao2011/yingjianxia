package com.yingjianxia.aftersales.feign;

import com.yingjianxia.aftersales.dto.RefundTriggerReq;
import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 担保资金服务 Feign 客户端（触发退款）
 */
@FeignClient(name = "escrow-service", path = "/api/v1/escrow", contextId = "escrowRefundClient",
        fallbackFactory = EscrowFeignClientFallbackFactory.class)
public interface EscrowFeignClient {

    /**
     * 触发退款（售后确认收货 / 仲裁支持买家 / 部分退款）
     */
    @PostMapping("/internal/refund")
    ApiResponse<String> triggerRefund(@RequestBody RefundTriggerReq req,
                                     @RequestHeader("X-Internal-Secret") String secret);
}
