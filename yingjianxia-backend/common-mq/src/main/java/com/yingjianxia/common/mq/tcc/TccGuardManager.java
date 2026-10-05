package com.yingjianxia.common.mq.tcc;

import com.yingjianxia.common.core.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * TCC 三阶段保障组件（幂等 + 悬挂防护 + 空回滚）
 * <p>
 * 以 idempotentKey 为核心，在 Redis 中存入三张标记：
 * <pre>
 *  yjx:tcc:try_mark:{key}    → Try 已执行标记（幂等）     TTL = 24h
 *  yjx:tcc:cancel_mark:{key} → Cancel 先到标记（悬挂防护）TTL = 24h
 *  yjx:tcc:confirm_mark:{key}→ Confirm 已执行（幂等）     TTL = 24h
 * </pre>
 *
 * <h3>三阶段流程</h3>
 * <ol>
 *   <li>Try 进入：<br>
 *       a. 若 cancel_mark 存在 → 说明 Cancel 先到（网络乱序）→ 拒绝 Try，抛出悬挂异常<br>
 *       b. 若 try_mark 存在 → 说明重复 Try → 直接返回上次结果（幂等）<br>
 *       c. SETNX try_mark 占位 → 执行业务</li>
 *   <li>Confirm 进入：<br>
 *       a. 若 confirm_mark 存在 → 重复 Confirm，直接返回（幂等）<br>
 *       b. 若 try_mark 不存在 → 说明 Try 没成功 → 空确认？通常视为异常，抛错</li>
 *   <li>Cancel 进入：<br>
 *       a. 若 cancel_mark 存在 → 幂等，返回<br>
 *       b. 若 try_mark 不存在 → Try 没执行（空回滚场景）<br>
 *          → 直接 SETNX cancel_mark 并返回成功（后续 Try 会被悬挂拦截拒绝）<br>
 *       c. SETNX cancel_mark → 执行解冻</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TccGuardManager {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    /** 阶段前缀常量 */
    private static final String TRY_PREFIX     = "yjx:tcc:try_mark:";
    private static final String CONFIRM_PREFIX = "yjx:tcc:confirm_mark:";
    private static final String CANCEL_PREFIX  = RedisKeyConstants.TCC_CANCEL_MARK.replace("%s", "");

    /* ============================================================
     *  Try 阶段
     * ============================================================ */

    /**
     * Try 前置检查。
     *
     * @return {@code true} 允许 Try，{@code false} 重复（幂等命中，可直接返回上次结果）
     * @throws TccHangException 命中 Cancel 先到（悬挂），禁止 Try
     */
    public boolean beforeTry(String idempotentKey, int ttlSeconds) throws TccHangException {
        // 1. 悬挂检测：Cancel 先到了
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(CANCEL_PREFIX + idempotentKey))) {
            log.warn("[TCC-悬挂] Try 进入时 Cancel 已先执行，key={}", idempotentKey);
            throw new TccHangException("TCC 悬挂防护：Cancel 先到，Try 被拒绝，key=" + idempotentKey);
        }
        // 2. 幂等检测：Try 已执行过
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(TRY_PREFIX + idempotentKey, "1", ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(first)) {
            log.info("[TCC-幂等] Try 重复进入，直接返回，key={}", idempotentKey);
            return false;
        }
        return true;
    }

    public void markTryResult(String idempotentKey, String resultJson, int ttlSeconds) {
        RBucket<String> bucket = redissonClient.getBucket(TRY_PREFIX + "result:" + idempotentKey);
        bucket.set(resultJson, ttlSeconds, TimeUnit.SECONDS);
    }

    public String loadTryResult(String idempotentKey) {
        RBucket<String> bucket = redissonClient.getBucket(TRY_PREFIX + "result:" + idempotentKey);
        return bucket.get();
    }

    /* ============================================================
     *  Confirm 阶段
     * ============================================================ */
    public boolean beforeConfirm(String idempotentKey, int ttlSeconds) {
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(CONFIRM_PREFIX + idempotentKey, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(first);
    }

    public boolean isTryExecuted(String idempotentKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(TRY_PREFIX + idempotentKey));
    }

    /* ============================================================
     *  Cancel 阶段
     * ============================================================ */

    /**
     * @return {@code true} 需要执行实际 Cancel；{@code false} 空回滚/幂等命中，应直接返回成功
     */
    public boolean beforeCancel(String idempotentKey, int ttlSeconds) {
        // 1. Cancel 幂等：已经 Cancel 过了
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(CANCEL_PREFIX + idempotentKey))) {
            log.info("[TCC-幂等] Cancel 重复进入，直接返回，key={}", idempotentKey);
            return false;
        }
        // 2. 空回滚：Try 没执行 → 先写 cancel_mark 占位，后续 Try 被悬挂拦截
        if (!isTryExecuted(idempotentKey)) {
            log.info("[TCC-空回滚] Try 未执行，Cancel 先到，写 cancel_mark 占位，key={}", idempotentKey);
            stringRedisTemplate.opsForValue()
                    .setIfAbsent(CANCEL_PREFIX + idempotentKey, "EMPTY_ROLLBACK", ttlSeconds, TimeUnit.SECONDS);
            return false;
        }
        // 3. 正常 Cancel：占位
        stringRedisTemplate.opsForValue()
                .setIfAbsent(CANCEL_PREFIX + idempotentKey, "1", ttlSeconds, TimeUnit.SECONDS);
        return true;
    }

    /**
     * 悬挂异常
     */
    public static class TccHangException extends RuntimeException {
        public TccHangException(String msg) { super(msg); }
    }
}
