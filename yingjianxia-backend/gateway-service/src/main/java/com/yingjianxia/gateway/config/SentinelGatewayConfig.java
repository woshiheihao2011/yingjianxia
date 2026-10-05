package com.yingjianxia.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网关 Sentinel 限流 / 熔断 配置
 * <p>
 * 包含：
 * <ul>
 *   <li>Sentinel SCG 适配器过滤器 & 异常处理器</li>
 *   <li>BlockRequestHandler 自定义 JSON 返回 (ApiResponse 格式，与全局异常一致)</li>
 *   <li>基础网关限流规则（按路由维度 + API 维度），动态规则建议接入 Nacos 动态数据源</li>
 * </ul>
 * <p>
 * 建议：生产环境规则均放到 Sentinel Dashboard / Nacos 配置中心下发，此配置为开发期默认兜底
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SentinelGatewayConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;
    private final ObjectMapper objectMapper;

    /**
     * Sentinel Gateway 过滤器 — 限流/熔断生效核心
     */
    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 限流/熔断后的异常处理器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler(
            RouteDefinitionLocator routeDefinitionLocator, GatewayProperties gatewayProperties) {
        // 注册自定义 Block 回调
        registerBlockCallback();
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 自定义限流/熔断返回 — 标准化 ApiResponse
     */
    private void registerBlockCallback() {
        BlockRequestHandler handler = (exchange, t) -> {
            ResultCode code = ResultCode.SERVICE_BLOCKED;
            // 根据原因区分返回码（默认 207 阻断；若降级类返回 SERVICE_DOWNGRADE）
            String kind = (t instanceof BlockException be && be.getRule() != null) ? be.getRule().getResource() : "flow";
            ApiResponse<Void> body = ApiResponse.fail(code.getCode(),
                    "请求过于频繁，已触发流控，请稍后重试 (rule=" + kind + ")");
            String json;
            try {
                json = objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                json = "{\"code\":207,\"message\":\"服务限流，请稍后重试\"}";
            }
            log.warn("【网关流控/熔断】命中规则: {}, traceId={}", kind,
                    exchange.getRequest().getHeaders().getFirst("X-Trace-Id"));
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(json));
        };
        GatewayCallbackManager.setBlockHandler(handler);
    }

    /* ============================================================
     *  开发期默认流控规则（加载到 Sentinel 内存）
     *  生产：请替换为 GatewayRuleManager.register2Property + NacosDataSource
     * ============================================================ */

    @jakarta.annotation.PostConstruct
    public void loadDefaultRules() {
        try {
            // 1. 按路由限流 — user-service 全站 500 QPS
            List<com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule> gwRules =
                    List.of(buildRouteRule("user-service-route", 500, 1));
            com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager.loadRules(new HashSet<>(gwRules));

            // 2. 按 API 维度：验证码发送接口 60 次/分钟（防刷，叠加幂等拦截器）
            Set<com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition> apis = new java.util.HashSet<>();
            com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition sendSmsApi =
                    new com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition("api_sms_send");
            sendSmsApi.setPredicateItems(new java.util.HashSet<>(Set.of(
                    new com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem()
                            .setPattern("/api/v1/auth/sms/send")
                            .setMatchStrategy(com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT)
            )));
            apis.add(sendSmsApi);
            com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager.loadApiDefinitions(apis);

            List<com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule> apiRules = List.of(
                    new com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule("api_sms_send")
                            .setResourceMode(com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                            .setCount(1)                           // 1 QPS
                            .setIntervalSec(60)                      // 每 60 秒窗口
                            .setParamItem(new com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem()
                                    .setParseStrategy(com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP))
            );
            // 合并规则
            java.util.List<com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule> all =
                    new java.util.ArrayList<>(gwRules);
            all.addAll(apiRules);
            com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager.loadRules(new HashSet<>(all));

            log.info("【Sentinel】网关默认流控规则加载完成");
        } catch (Exception e) {
            log.warn("【Sentinel】默认规则加载失败（不影响启动，生产环境由 Nacos 动态下发）", e);
        }
    }

    private com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule buildRouteRule(
            String routeId, int count, int intervalSec) {
        return new com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule(routeId)
                .setResourceMode(com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                .setCount(count)
                .setIntervalSec(intervalSec)
                .setControlBehavior(com.alibaba.csp.sentinel.slots.block.RuleConstant.CONTROL_BEHAVIOR_DEFAULT)
                .setGrade(com.alibaba.csp.sentinel.slots.block.RuleConstant.FLOW_GRADE_QPS);
    }
}
