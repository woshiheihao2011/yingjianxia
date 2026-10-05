package com.yingjianxia.common.mq.tcc;

import java.lang.annotation.*;

/**
 * TCC 事务动作注解（Try 入口）
 * <p>
 * 标注在业务 Service 的 Try 方法上，由 TccAspect 环绕拦截，自动完成：
 * <ol>
 *   <li>幂等校验：同一 idempotentKey 已 Try/Confirm 过则直接返回原结果</li>
 *   <li>悬挂防护：若 Cancel 先执行（网络异常导致框架先调 Cancel），Try 会检查 cancel_mark 直接拒绝</li>
 *   <li>空回滚防护：若 Try 未执行 Cancel 先到，标记 cancel_mark，后续 Try 直接拒绝</li>
 * </ol>
 * <h3>约束</h3>
 * <ul>
 *   <li>方法参数中必须包含 {@link TccContext}，或可通过 {@link #idempotentKeySpel()} 解析出幂等键</li>
 *   <li>必须存在对应的 confirmMethod / cancelMethod</li>
 * </ul>
 * <h3>使用示例</h3>
 * <pre>
 * &#64;TccTransactional(
 *     confirmMethod = "confirmFreeze",
 *     cancelMethod  = "cancelFreeze",
 *     idempotentKeySpel = "#context.idempotentKey"
 * )
 * public EscrowResult tryFreeze(TccContext context, Long buyerId, BigDecimal amount) {
 *     // Try: 可用余额 -amount, 冻结余额 +amount
 * }
 * public EscrowResult confirmFreeze(TccContext context, ...) { }
 * public EscrowResult cancelFreeze(TccContext context, ...)  { }
 * </pre>
 *
 * @author 硬件侠后端团队
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TccTransactional {

    /** 事务名称，默认取 "类名#方法名" */
    String name() default "";

    /** Confirm 方法名（同服务类内） */
    String confirmMethod();

    /** Cancel 方法名（同服务类内） */
    String cancelMethod();

    /**
     * SpEL 表达式，用于从方法参数解析幂等键；
     * 默认查找第一个 TccContext 类型参数的 idempotentKey。
     */
    String idempotentKeySpel() default "";

    /** 幂等键 Redis TTL（秒），默认 24h（容忍超久的网络乱序） */
    int keyTtlSeconds() default 24 * 3600;
}
