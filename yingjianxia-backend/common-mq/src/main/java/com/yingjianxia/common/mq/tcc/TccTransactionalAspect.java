package com.yingjianxia.common.mq.tcc;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * TccTransactional 注解环绕 AOP 切面
 * <p>
 * 拦截 Try 方法，统一完成 幂等 + 悬挂 + 空回滚标记的 Redis 守护。
 * Confirm/Cancel 由事务管理器（Seata 或自研 Saga 协调器）通过反射调用对应方法，
 * 建议在 Confirm/Cancel 方法内部显式调用 {@link TccGuardManager} 检查。
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class TccTransactionalAspect {

    private final TccGuardManager guardManager;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    @Around("@annotation(anno)")
    public Object aroundTry(ProceedingJoinPoint pjp, TccTransactional anno) throws Throwable {
        String idempotentKey = resolveIdempotentKey(anno, pjp);
        int ttl = anno.keyTtlSeconds();
        log.debug("[TCC-AOP] Try 进入 key={}, method={}", idempotentKey, pjp.getSignature());

        boolean firstTry;
        try {
            firstTry = guardManager.beforeTry(idempotentKey, ttl);
        } catch (TccGuardManager.TccHangException hang) {
            throw hang;
        }

        if (!firstTry) {
            // 幂等：尝试读取上次结果返回
            String cached = guardManager.loadTryResult(idempotentKey);
            if (cached != null) {
                log.info("[TCC-AOP] Try 幂等命中，返回缓存结果，key={}", idempotentKey);
                Class<?> returnType = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getReturnType();
                try {
                    return JSONUtil.toBean(cached, returnType);
                } catch (Exception ignore) {
                    return cached;
                }
            }
            // 没缓存但标记存在：业务层自行判断（通常 Try 内部 DB 幂等键也会拦住）
        }

        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            // 业务异常：Try 没成功，需要清除 try_mark 让 Cancel 走空回滚分支？
            // 一般不主动清除，让业务层的 DB 事务回滚保证最终一致；此处仅日志
            log.warn("[TCC-AOP] Try 业务异常，key={}, msg={}", idempotentKey, t.getMessage());
            throw t;
        }

        // 缓存结果（防重复 Try 时可直接返回）
        try {
            guardManager.markTryResult(idempotentKey, JSONUtil.toJsonStr(result), ttl);
        } catch (Exception ignore) {}

        return result;
    }

    /* ---------- 幂等键解析 ---------- */

    private String resolveIdempotentKey(TccTransactional anno, ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        // 1. SpEL 显式指定
        if (anno.idempotentKeySpel() != null && !anno.idempotentKeySpel().isBlank()) {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            Method m = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getMethod();
            String[] names = new org.springframework.core.DefaultParameterNameDiscoverer().getParameterNames(m);
            if (names != null) {
                for (int i = 0; i < names.length; i++) ctx.setVariable(names[i], args[i]);
            }
            ctx.setVariable("context", findTccContext(args));
            try {
                Object v = spelParser.parseExpression(anno.idempotentKeySpel()).getValue(ctx);
                if (v != null && !v.toString().isBlank()) return v.toString();
            } catch (Exception ignore) {}
        }

        // 2. 取第一个 TccContext 参数的 idempotentKey
        TccContext ctx = findTccContext(args);
        if (ctx != null && ctx.getIdempotentKey() != null && !ctx.getIdempotentKey().isBlank()) {
            return ctx.getIdempotentKey();
        }

        // 3. 兜底：类名#方法名#参数hash（不推荐使用，日志提示）
        log.warn("[TCC-AOP] 幂等键未显式指定，使用兜底方案，method={}", pjp.getSignature());
        return pjp.getSignature().toLongString();
    }

    private TccContext findTccContext(Object[] args) {
        for (Object a : args) {
            if (a instanceof TccContext ctx) return ctx;
        }
        return null;
    }
}
