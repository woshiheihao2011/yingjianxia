package com.yingjianxia.common.web.interceptor;

import cn.hutool.core.util.StrUtil;
import com.yingjianxia.common.core.constants.BusinessConstants;
import com.yingjianxia.common.core.constants.RedisKeyConstants;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 幂等键拦截器
 * <p>
 * 从请求 Header {@code X-Idempotency-Key} 读取幂等键，使用 Redis SETNX 原子占位，
 * 若占位失败则认为是重复请求，抛出 {@code IDEMPOTENT_KEY_DUPLICATE} 错误。
 * </p>
 * <p>
 * 使用方式（WebMvcConfig）：
 * <pre>
 *   registry.addInterceptor(idempotentInterceptor)
 *           .addPathPatterns("/api/v1/**")
 *           .excludePathPatterns("/api/v1/users/login", "/api/v1/users/register");
 * </pre>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // GET / HEAD / OPTIONS 只读请求不做幂等
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String key = request.getHeader(BusinessConstants.HDR_IDEMPOTENT);

        // 若用户未传幂等键，则使用 方法+请求URI+userId 作为兜底键（允许连续发3秒内重复）
        if (StrUtil.isBlank(key)) {
            String uid = request.getHeader(BusinessConstants.HDR_USER_ID);
            key = method + ":" + request.getRequestURI() + ":" + (uid != null ? uid : getClientIp(request));
        }

        String redisKey = String.format(RedisKeyConstants.API_IDEMPOTENT, key);

        // 优先 SETNX
        Boolean set = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", BusinessConstants.IDEMPOTENT_DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(set)) {
            log.warn("[幂等拦截] 重复请求 key={}, uri={}", key, request.getRequestURI());
            throw new BusinessException(ResultCode.IDEMPOTENT_KEY_DUPLICATE);
        }
        return true;
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader(BusinessConstants.HDR_FORWARDED_FOR);
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader(BusinessConstants.HDR_REAL_IP);
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        return ip;
    }
}
