package com.yingjianxia.common.web.interceptor;

import cn.hutool.core.util.StrUtil;
import com.yingjianxia.common.core.constants.BusinessConstants;
import com.yingjianxia.common.core.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求上下文拦截器
 * <p>
 * 从网关注入的请求头中解析用户ID、角色、TraceId、灰度标记、客户端真实IP，
 * 放入 {@link UserContext} ThreadLocal，请求结束时自动清理。
 * </p>
 * <p>
 * 注意：网关必须校验 Token 才会注入 X-User-Id；
 * 白名单接口（登录/注册/首页等）不会注入 UserId，UserContext#userId 为 null。
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext ctx = new UserContext();

        String uidStr = request.getHeader(BusinessConstants.HDR_USER_ID);
        if (StrUtil.isNotBlank(uidStr)) {
            try {
                ctx.setUserId(Long.parseLong(uidStr.trim()));
            } catch (NumberFormatException e) {
                log.warn("[上下文] X-User-Id 非法: {}", uidStr);
            }
        }
        ctx.setRoles(request.getHeader(BusinessConstants.HDR_USER_ROLES));
        ctx.setTraceId(request.getHeader(BusinessConstants.HDR_TRACE_ID));
        ctx.setGray("gray".equalsIgnoreCase(request.getHeader(BusinessConstants.HDR_GRAY_FLAG)));
        ctx.setClientIp(getClientIp(request));

        UserContext.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader(BusinessConstants.HDR_FORWARDED_FOR);
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = req.getHeader(BusinessConstants.HDR_REAL_IP);
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return req.getRemoteAddr();
    }
}
