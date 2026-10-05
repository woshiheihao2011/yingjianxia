package com.yingjianxia.common.feign.fallback;

import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Sentinel 降级/熔断通用工具
 * <p>
 * Feign Fallback 类中出现异常后，使用此类的工厂方法产出统一降级结果，
 * 避免各服务重复造轮子。
 * </p>
 *
 * <h3>典型用法</h3>
 * <pre>
 * &#64;Component
 * public class ProductFeignFallback implements ProductFeignClient {
 *     &#64;Override
 *     public ApiResponse&lt;ProductVO&gt; getProduct(Long id) {
 *         return SentinelFallback.of("product-service#getProduct", "id=" + id, () -> null);
 *     }
 * }
 * </pre>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
public final class SentinelFallback {

    private SentinelFallback() {}

    /**
     * 标准降级：返回 ApiResponse 结构，code = SERVICE_DOWNGRADE
     */
    public static <T> ApiResponse<T> response(String client, String method, String params) {
        log.warn("[Sentinel 降级] client={}, method={}, params={}", client, method, params);
        return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE, "下游服务暂不可用");
    }

    /**
     * 提供默认值的降级
     */
    public static <T> ApiResponse<T> response(String client, String method, String params, Supplier<T> defaultValue) {
        log.warn("[Sentinel 降级] client={}, method={}, params={} → 使用默认值", client, method, params);
        try {
            return ApiResponse.success(defaultValue.get());
        } catch (Exception e) {
            return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE, "服务降级且默认值构造失败");
        }
    }

    /**
     * 非 ApiResponse 的 Feign 调用返回业务 DTO：直接抛 BusinessException 给上层全局异常处理
     */
    public static <T> T throwFallback(String client, String method, String params) {
        log.warn("[Sentinel 降级] client={}, method={}, params={} → 抛出业务异常", client, method, params);
        throw new BusinessException(ResultCode.SERVICE_DOWNGRADE);
    }
}
