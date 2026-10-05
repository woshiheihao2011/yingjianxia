package com.yingjianxia.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 灰度发布全局过滤器
 * <p>
 * 灰度策略（支持两种方式，可组合）：
 * <ul>
 *   <li>Header 驱动：请求带 X-Gray: true → 强制路由到 metadata.gray=true 的实例（运维调试用）</li>
 *   <li>比例分流：基于 X-User-Id，10% 用户路由到灰度版本（userId % 10 == 0），比例通过配置调整</li>
 * </ul>
 * 实现方式：在 ServerHttpRequest Attributes 中写入 {@code gray} 属性，
 * 由 {@link GrayLoadBalancer} 在选择实例时根据 metadata 判断。
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
public class GrayGlobalFilter implements GlobalFilter, Ordered {

    /** 请求属性 key：是否灰度流量 */
    public static final String ATTR_GRAY = "yjx-gray";
    /** 默认灰度比例：10% */
    private static final int GRAY_RATIO_PERCENT = 10;

    @Override
    public int getOrder() {
        // 在 JWT 过滤器之后，路由之前
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        boolean gray = false;

        // 1. X-Gray header 优先
        String grayHeader = req.getHeaders().getFirst("X-Gray");
        if ("true".equalsIgnoreCase(grayHeader) || "1".equals(grayHeader)) {
            gray = true;
        }

        // 2. 比例分流（基于 userId）
        if (!gray) {
            String userId = req.getHeaders().getFirst("X-User-Id");
            if (StrUtil.isNotBlank(userId)) {
                try {
                    long id = Long.parseLong(userId);
                    if ((id % 100) < GRAY_RATIO_PERCENT) {
                        gray = true;
                    }
                } catch (NumberFormatException ignore) {
                }
            }
        }

        if (gray) {
            exchange.getAttributes().put(ATTR_GRAY, true);
            if (log.isDebugEnabled()) {
                log.debug("【灰度】命中灰度流量, traceId={}", req.getHeaders().getFirst("X-Trace-Id"));
            }
        }

        return chain.filter(exchange);
    }
}
