package com.yingjianxia.aftersales.feign;

import com.yingjianxia.aftersales.dto.RefundTriggerReq;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Escrow Feign 客户端降级工厂 — escrow-service 不可用时返回失败响应
 */
@Slf4j
@Component
public class EscrowFeignClientFallbackFactory implements FallbackFactory<EscrowFeignClient> {

    @Override
    public EscrowFeignClient create(Throwable cause) {
        log.error("[EscrowFeign] 触发退款调用失败，触发降级 cause={}", cause.getMessage());
        return new EscrowFeignClient() {
            @Override
            public ApiResponse<String> triggerRefund(RefundTriggerReq req, String secret) {
                return ApiResponse.fail(ResultCode.THIRD_PARTY_CALL_FAIL,
                        "退款服务调用失败：" + (cause == null ? "unknown" : cause.getMessage()));
            }
        };
    }
}
