package com.yingjianxia.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.constants.RedisKeyConstants;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import com.yingjianxia.common.core.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * JWT 全局鉴权过滤器
 * <p>
 * 执行顺序：
 * <pre>
 *   CORS → JWT → 灰度 → Sentinel → 路由 → 下游服务
 *   Ordered.HIGHEST_PRECEDENCE + 10
 * </pre>
 *
 * <ul>
 *   <li>白名单路径（/api/v1/auth/**、/doc.html 等）直接放行</li>
 *   <li>非白名单必须携带 Authorization: Bearer xxx，Access Token</li>
 *   <li>解析 JWT 后校验 jti 是否在 Redis 黑名单</li>
 *   <li>注入 X-User-Id / X-User-Roles / X-Trace-Id / X-Client-Ip / X-Gray 供下游服务读取</li>
 *   <li>未登录/过期/非法/吊销 → 返回 401 + ApiResponse JSON</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    /**
     * 白名单 — 无需登录即可访问
     * 注意：不要加 /api/v1/product/ 或 /api/v1/shop/ 这种过宽前缀，
     * 否则卖家端接口（/product/mine, /shop/mine 等）也会跳过 JWT 鉴权，
     * 导致下游服务拿不到 X-User-Id 头。
     */
    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/v1/auth/sms/",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/token/refresh",
            "/api/v1/auth/oauth",
            // Knife4j / Spring Doc
            "/doc.html",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/favicon.ico",
            // Sentinel endpoint
            "/actuator",
            // 商品公开查询（仅搜索，详情通过 /{id} 数字匹配走鉴权后放行）
            "/api/v1/product/search",
            "/api/v1/product/categories",
            "/api/v1/categories",
            // 店铺公开查询（买家浏览用，按 ID 查店铺）
            "/api/v1/shop/by-seller/",
            // 支付回调/验机回调（含幂等键二次校验）
            "/api/v1/payment/notify/",
            "/api/v1/inspection/callback/",
            // 验机模板公开查询
            "/api/v1/inspection/templates",
            // 验机报告公开查询（商品详情页/订单详情，按商品ID或报告ID查公开摘要）
            "/api/v1/inspection/report/by-product/",
            "/api/v1/inspection/report/verify",
            // 评价列表公开查询（精确匹配，不覆盖 /reviews/mine 等需要鉴权的子路径）
            "/api/v1/evaluation/products/",
            // 店铺评价公开查询（按卖家ID查评价列表/统计）
            "/api/v1/evaluation/sellers/",
            // 店铺上传图片（Logo/Banner 等静态资源，公开访问）
            "/uploads/"
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. TraceId 贯穿（无论是否登录都生成）
        String traceId = StrUtil.blankToDefault(request.getHeaders().getFirst("X-Trace-Id"),
                UUID.randomUUID().toString().replace("-", ""));
        String clientIp = resolveClientIp(request);
        String gray = request.getHeaders().getFirst("X-Gray");

        ServerHttpRequest.Builder mutated = request.mutate()
                .header("X-Trace-Id", traceId)
                .header("X-Client-Ip", clientIp);
        if (StrUtil.isNotBlank(gray)) {
            mutated.header("X-Gray", gray);
        }

        // 2. 白名单直接放行（仍然注入 TraceId）
        if (isWhitelist(path)) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        // 2.1 商品详情 GET /api/v1/product/{数字} 公开访问（但如果带了 Token 仍解析注入 X-User-Id）
        if ("GET".equalsIgnoreCase(request.getMethod().name()) && isPublicProductDetail(path)) {
            // 如果带了 Token，尝试解析并注入 X-User-Id（匿名浏览也允许）
            String authHeader2 = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StrUtil.isNotBlank(authHeader2) && authHeader2.startsWith("Bearer ")) {
                try {
                    Claims c = jwtUtils.parseToken(authHeader2.substring(7));
                    mutated.header("X-User-Id", c.getSubject());
                } catch (Exception ignored) {
                    // Token 无效时仍允许匿名浏览
                }
            }
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        // 2.2 店铺详情 GET /api/v1/shop/{数字} 公开访问
        if ("GET".equalsIgnoreCase(request.getMethod().name()) && isPublicShopDetail(path)) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        // 2.3 社区公开浏览（帖子列表/详情/热门话题/排行榜，排除 /posts/mine）
        if ("GET".equalsIgnoreCase(request.getMethod().name()) && isPublicCommunityPath(path)) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        // 2.4 公告公开浏览（列表 + 详情）
        if ("GET".equalsIgnoreCase(request.getMethod().name()) && isPublicAnnouncementPath(path)) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        // 3. OPTIONS 预检请求放行（CORS）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        // 4. 提取 Authorization Bearer Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            return writeUnauthorized(exchange, ResultCode.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);

        // 5. 必须是 Access Token（禁止把 Refresh Token 当业务凭证）
        if (!jwtUtils.isAccessToken(token)) {
            return writeUnauthorized(exchange, ResultCode.TOKEN_INVALID, "请使用 Access Token 访问业务接口");
        }

        // 6. 解析 & 过期校验
        Claims claims;
        try {
            claims = jwtUtils.parseToken(token);
        } catch (JwtException e) {
            log.warn("【网关】JWT 解析失败 traceId={}, err={}", traceId, e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("expire")) {
                return writeUnauthorized(exchange, ResultCode.TOKEN_EXPIRED);
            }
            return writeUnauthorized(exchange, ResultCode.TOKEN_INVALID);
        }

        // 7. 黑名单检查（Redis SET/Bucket）
        String jti = claims.getId();
        RBucket<String> black = redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti));
        if (black.isExists()) {
            return writeUnauthorized(exchange, ResultCode.TOKEN_INVALID);
        }

        // 8. 解析 Claims → 请求头
        String userId = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        String rolesStr = (roles == null || roles.isEmpty()) ? "" : String.join(",", roles);

        mutated.header("X-User-Id", userId);
        mutated.header("X-User-Roles", rolesStr);

        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    /* ======================== 工具 ======================== */

    private boolean isWhitelist(String path) {
        for (String p : WHITELIST_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    /** 判断是否为商品详情公开路径：GET /api/v1/product/{数字} */
    private boolean isPublicProductDetail(String path) {
        // 匹配 /api/v1/product/123 （数字 ID），不匹配 /api/v1/product/mine 等
        if (!path.startsWith("/api/v1/product/")) return false;
        String rest = path.substring("/api/v1/product/".length());
        // 去掉可能的 query string
        int q = rest.indexOf('?');
        if (q >= 0) rest = rest.substring(0, q);
        // 纯数字才放行（mine, internal 等不放行）
        return rest.matches("\\d+");
    }

    /** 判断是否为店铺详情公开路径：GET /api/v1/shop/{数字} 及其公开子路径（/stats、/reviews、/reviews/stats） */
    private boolean isPublicShopDetail(String path) {
        if (!path.startsWith("/api/v1/shop/")) return false;
        String rest = path.substring("/api/v1/shop/".length());
        int q = rest.indexOf('?');
        if (q >= 0) rest = rest.substring(0, q);
        // 匹配纯数字 ID（店铺详情）或 数字 ID 后跟 /stats、/reviews、/reviews/stats
        return rest.matches("\\d+(/(stats|reviews(/stats)?)?)?");
    }

    /** 判断是否为社区公开浏览路径（GET 请求，排除 /posts/mine 等需鉴权子路径） */
    private boolean isPublicCommunityPath(String path) {
        if (!path.startsWith("/api/v1/community/")) return false;
        String rest = path.substring("/api/v1/community/".length());
        int q = rest.indexOf('?');
        if (q >= 0) rest = rest.substring(0, q);
        // 排除需鉴权的子路径
        if (rest.equals("posts/mine") || rest.startsWith("posts/mine/")) return false;
        // 公开：帖子列表、帖子详情(数字ID)、热门话题、排行榜
        return rest.equals("posts")
                || rest.matches("posts/\\d+")
                || rest.equals("hot-topics")
                || rest.equals("ranks");
    }

    /** 判断是否为公告公开浏览路径（GET 请求，列表 + 数字ID详情） */
    private boolean isPublicAnnouncementPath(String path) {
        if (!path.startsWith("/api/v1/announcements")) return false;
        String rest = path.substring("/api/v1/announcements".length());
        int q = rest.indexOf('?');
        if (q >= 0) rest = rest.substring(0, q);
        // 公开：空（列表根路径）或 /{数字}（详情）
        return rest.isEmpty() || rest.matches("/\\d+");
    }

    private String resolveClientIp(ServerHttpRequest req) {
        String ip = req.getHeaders().getFirst("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = req.getHeaders().getFirst("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) return ip;
        if (req.getRemoteAddress() != null) {
            return req.getRemoteAddress().getAddress().getHostAddress();
        }
        return "0.0.0.0";
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, ResultCode code) {
        return writeUnauthorized(exchange, code, code.getMessage());
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, ResultCode code, String msg) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiResponse<Void> body = ApiResponse.fail(code.getCode(), msg);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = resp.bufferFactory().wrap(bytes);
            return resp.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            DataBuffer buffer = resp.bufferFactory().wrap(
                    ("{\"code\":" + code.getCode() + ",\"message\":\"" + msg + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
            return resp.writeWith(Mono.just(buffer));
        }
    }
}
