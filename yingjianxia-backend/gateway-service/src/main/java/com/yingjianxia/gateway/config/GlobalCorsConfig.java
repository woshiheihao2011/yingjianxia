package com.yingjianxia.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 全局 CORS 跨域配置
 * <p>
 * 说明：
 * <ul>
 *   <li>生产环境 allowedOrigins 需要收敛到具体域名；开发期放开 * 方便联调</li>
 *   <li>允许 Authorization / X-User-Id / X-Gray / X-Idempotent-Key / X-Trace-Id 等自定义头</li>
 *   <li>预检请求缓存 1 小时，减少 OPTIONS 次数</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        // TODO: 生产改为精确域名列表（白名单），如 https://www.yingjianxia.com
        cfg.addAllowedOriginPattern("*");
        cfg.setAllowCredentials(true);

        cfg.addAllowedMethod(HttpMethod.GET);
        cfg.addAllowedMethod(HttpMethod.POST);
        cfg.addAllowedMethod(HttpMethod.PUT);
        cfg.addAllowedMethod(HttpMethod.DELETE);
        cfg.addAllowedMethod(HttpMethod.PATCH);
        cfg.addAllowedMethod(HttpMethod.OPTIONS);
        cfg.addAllowedMethod(HttpMethod.HEAD);

        // 允许的请求头：默认 + 业务自定义
        cfg.addAllowedHeader("*");

        // 前端可读取的响应头
        cfg.addExposedHeader("Content-Disposition");
        cfg.addExposedHeader("X-Trace-Id");
        cfg.addExposedHeader("X-Request-Id");

        // 预检缓存 3600s
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(source);
    }
}
