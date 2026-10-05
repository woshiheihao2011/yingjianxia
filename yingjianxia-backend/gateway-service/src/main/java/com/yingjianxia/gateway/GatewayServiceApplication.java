package com.yingjianxia.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类 — WebFlux 栈
 * <p>
 * 职责：
 * <ul>
 *   <li>路由转发（lb:// 服务名）</li>
 *   <li>JWT 鉴权过滤器：解析、黑名单、注入请求头 X-User-Id/X-User-Roles/X-Trace-Id</li>
 *   <li>Sentinel 限流熔断：QPS 限流、热点参数限流、降级</li>
 *   <li>CORS 全局配置</li>
 *   <li>灰度路由：按 X-Gray Header 或 userId 百分比路由到 gray 版本实例</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.gateway",
        "com.yingjianxia.common.core"
})
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — API 网关 (gateway-service) 启动成功
                   HTTP 端口: 8080 (默认)
                   路由入口:  http://localhost:8080/{service-prefix}/**
                   Nacos:    已注册为 gateway-service
                   Sentinel: 限流/熔断规则已加载（见 sentinel 控制台 8858）
                =========================================================
                """);
    }
}
