package com.yingjianxia.common.feign.config;

import com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration;
import com.yingjianxia.common.feign.interceptor.FeignHeaderRelayInterceptor;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

/**
 * Feign 自动配置
 * <p>
 * 统一装配：
 * <ul>
 *   <li>请求头透传拦截器 FeignHeaderRelayInterceptor</li>
 *   <li>Feign 重试策略：默认不重试（保障资金操作幂等安全），可在各业务显式覆盖</li>
 *   <li>整合 Sentinel 支持：导入 SentinelFeignAutoConfiguration</li>
 *   <li>Feign Client 超时：默认 connect=2s, read=5s</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@AutoConfiguration
@Import(SentinelFeignAutoConfiguration.class)
@ConditionalOnClass(feign.Feign.class)
public class DefaultFeignAutoConfig {

    /**
     * 请求头透传拦截器（注册到所有 Feign Client）
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor feignHeaderRelayInterceptor() {
        return new FeignHeaderRelayInterceptor();
    }

    /**
     * 全局不重试（默认）
     * <p>
     * 资金类/下单类接口一旦被重复调用会出问题，因此默认禁止重试；
     * 需要重试的业务可在 @FeignClient 上单独配置 configuration 覆盖。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 超时配置示例（也可以在 yml 中通过 feign.client.config.default 配置）
     */
    @Bean
    public feign.Request.Options feignRequestOptions() {
        return new feign.Request.Options(
                2, TimeUnit.SECONDS,
                5, TimeUnit.SECONDS,
                true
        );
    }
}
