package com.yingjianxia.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerEnvironmentPropertyUtils;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 灰度负载均衡器 — 按 GrayGlobalFilter 的标记选择实例
 * <ul>
 *   <li>命中灰度流量：优选 {@code metadata.gray=true} 的实例，若不存在则回退到全部实例随机</li>
 *   <li>普通流量：仅选 {@code metadata.gray!=true}（即排除灰度实例），若全部为灰度实例则兼容选择</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
public class GrayBasedLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ServiceInstanceListSupplier supplier;
    private final AtomicInteger position = new AtomicInteger(new Random().nextInt(1000));
    private static final String GRAY_META_KEY = "gray";

    public GrayBasedLoadBalancer(ServiceInstanceListSupplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        if (supplier instanceof NoopServiceInstanceListSupplier) {
            return Mono.just(new EmptyResponse());
        }

        // 从请求上下文读取灰度标记（可通过 Request 的 context 透传，这里基于默认简化：
        // 实际生产可结合 ServerWebExchange + Reactor Context，此处做 fallback）
        Object ctx = request.getContext();
        final boolean isGrayFlow = (ctx instanceof org.springframework.web.server.ServerWebExchange exchange)
                && Boolean.TRUE.equals(exchange.getAttributeOrDefault(GrayGlobalFilter.ATTR_GRAY, false));

        return supplier.get().next().map(instances -> {
            if (CollUtil.isEmpty(instances)) {
                log.warn("【负载均衡】无可用实例");
                return new EmptyResponse();
            }

            List<ServiceInstance> candidates;
            if (isGrayFlow) {
                candidates = instances.stream()
                        .filter(i -> "true".equalsIgnoreCase(meta(i, GRAY_META_KEY)))
                        .toList();
                if (candidates.isEmpty()) candidates = instances; // 回退：无灰度实例时全部可用
            } else {
                candidates = instances.stream()
                        .filter(i -> !"true".equalsIgnoreCase(meta(i, GRAY_META_KEY)))
                        .toList();
                if (candidates.isEmpty()) candidates = instances; // 兼容：所有都是灰度实例
            }

            // 轮询 + 随机扰动
            int idx = Math.abs(position.incrementAndGet() % candidates.size());
            ServiceInstance chosen = candidates.get(idx);
            log.debug("【负载均衡】灰度={}, 选中实例={}:{}", isGrayFlow, chosen.getHost(), chosen.getPort());
            return new DefaultResponse(chosen);
        });
    }

    private static String meta(ServiceInstance i, String key) {
        if (i.getMetadata() == null) return null;
        return i.getMetadata().get(key);
    }
}
