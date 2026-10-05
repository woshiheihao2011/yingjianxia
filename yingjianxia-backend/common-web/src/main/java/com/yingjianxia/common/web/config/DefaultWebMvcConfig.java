package com.yingjianxia.common.web.config;

import com.yingjianxia.common.core.constants.BusinessConstants;
import com.yingjianxia.common.web.interceptor.IdempotentInterceptor;
import com.yingjianxia.common.web.interceptor.RequestContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用 WebMvc 配置
 * <p>
 * 提供：
 * <ul>
 *   <li>CORS 跨域配置（默认允许所有源，生产环境建议收紧到具体域名）</li>
 *   <li>请求上下文拦截器（所有路径注册）</li>
 *   <li>幂等键拦截器（默认开启所有 POST/PUT/DELETE，白名单接口排除）</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Configuration
@RequiredArgsConstructor
public class DefaultWebMvcConfig implements WebMvcConfigurer {

    private final RequestContextInterceptor requestContextInterceptor;
    private final IdempotentInterceptor idempotentInterceptor;

    /**
     * 默认白名单（上下文/幂等均不拦截）
     */
    private static final String[] DEFAULT_WHITELIST = new String[] {
            // 认证相关
            "/api/v1/users/login",
            "/api/v1/users/register",
            "/api/v1/users/sms",
            "/api/v1/users/sms/verify",
            "/api/v1/users/password/reset",
            "/api/v1/oauth/**",
            // 商品浏览（公开）
            "/api/v1/products/categories/**",
            "/api/v1/products/search",
            "/api/v1/products/{id}",
            // 验机报告（公开）
            "/api/v1/inspections/{productId}/report",
            // 公告/FAQ/帮助中心
            "/api/v1/notices/**",
            "/api/v1/faqs/**",
            "/api/v1/help/**",
            // 文件/图片上传（无需幂等键，允许连续上传多张）
            "/api/v1/file/upload",
            "/api/v1/shop/upload",
            // Knife4j & Actuator
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/favicon.ico",
            "/actuator/**",
            // 支付回调（第三方平台调用，验签走内部逻辑）
            "/api/v1/payment/callback/**",
            // 内部服务间调用（Feign，无需认证）
            "/api/v1/product/internal/**"
    };

    /* ---------- CORS ---------- */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders(BusinessConstants.HDR_TRACE_ID, BusinessConstants.HDR_IDEMPOTENT)
                .allowCredentials(true)
                .maxAge(3600L);
    }

    /* ---------- 拦截器注册 ---------- */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. 请求上下文：优先注册，全路径（不排除任何路径；
        //    公开接口无 X-User-Id 时 userId 保持 null，是安全的）
        registry.addInterceptor(requestContextInterceptor)
                .addPathPatterns("/**")
                .order(0);

        // 2. 幂等键拦截：注册在上下文之后，白名单接口无需幂等键
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns(BusinessConstants.API_PREFIX + "/**")
                .excludePathPatterns(DEFAULT_WHITELIST)
                .order(1);
    }
}
