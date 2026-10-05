package com.yingjianxia.common.feign.interceptor;

import cn.hutool.core.util.StrUtil;
import com.yingjianxia.common.core.constants.BusinessConstants;
import com.yingjianxia.common.core.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 请求拦截器 — 把当前请求的关键信息透传到下游服务
 * <p>
 * 透传以下 Header：
 * <ul>
 *   <li>X-User-Id      ：用户ID（来自网关解析后的 UserContext）</li>
 *   <li>X-User-Roles    ：用户角色</li>
 *   <li>X-Trace-Id      ：链路追踪ID</li>
 *   <li>X-Gray-Flag     ：灰度标记</li>
 *   <li>X-Idempotency-Key：幂等键（防止级联重复调用）</li>
 * </ul>
 * <p>
 * 注意：下游服务必须有对应的白名单或信任内部调用。
 *
 * @author 硬件侠后端团队
 */
@Slf4j
public class FeignHeaderRelayInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        UserContext ctx = null;
        try {
            ctx = UserContext.get();
        } catch (Exception ignore) {
            // 非请求线程中调用（如异步任务）
        }

        if (ctx != null) {
            if (ctx.getUserId() != null) {
                template.header(BusinessConstants.HDR_USER_ID, String.valueOf(ctx.getUserId()));
            }
            if (StrUtil.isNotBlank(ctx.getRoles())) {
                template.header(BusinessConstants.HDR_USER_ROLES, ctx.getRoles());
            }
            if (StrUtil.isNotBlank(ctx.getTraceId())) {
                template.header(BusinessConstants.HDR_TRACE_ID, ctx.getTraceId());
            }
            if (ctx.isGray()) {
                template.header(BusinessConstants.HDR_GRAY_FLAG, "gray");
            }
        }

        // 如果有幂等键，自动构造下游键（源键+目标服务+接口）
        String idempotentKey = getInheritedIdempotentKey(template);
        if (StrUtil.isNotBlank(idempotentKey)) {
            template.header(BusinessConstants.HDR_IDEMPOTENT, idempotentKey);
        }
    }

    private String getInheritedIdempotentKey(RequestTemplate template) {
        String srcKey = null;
        try {
            UserContext ctx = UserContext.get();
            if (ctx.getTraceId() != null) srcKey = ctx.getTraceId();
        } catch (Exception ignore) {}
        if (StrUtil.isBlank(srcKey)) return null;
        // 简单构造：traceId + method + path
        return "feign:" + srcKey + ":" + template.method() + ":" + template.path();
    }
}
