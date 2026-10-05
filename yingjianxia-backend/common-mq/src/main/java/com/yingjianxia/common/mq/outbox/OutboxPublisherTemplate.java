package com.yingjianxia.common.mq.outbox;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yingjianxia.common.core.event.BaseDomainEvent;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Pattern 发布器模板
 * <p>
 * 使用步骤：
 * <ol>
 *   <li>业务方法中标注 @Transactional</li>
 *   <li>写业务数据（订单 / 钱包变更等）</li>
 *   <li>调用 {@link #save(BaseDomainEvent, String, String)} 把事件写入 outbox_events 表</li>
 *   <li>事务提交成功后，Outbox事件一定落库；XXL-Job 定时扫描并发送 MQ</li>
 *   <li>发送成功后置 status=SENT，失败指数退避重试</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisherTemplate {

    private final BaseRepository<OutboxEvent> outboxMapper;

    /** 最大重试次数：15 次，指数退避约几小时 */
    private static final int MAX_RETRY = 15;

    /* ==================== 保存 Outbox 事件 ==================== */

    /**
     * 业务事务内调用：写入本地消息表（幂等）
     */
    @Transactional(rollbackFor = Exception.class)
    public <T extends BaseDomainEvent> OutboxEvent save(T event, String topic, String tag) {
        if (event == null || StrUtil.isBlank(topic)) {
            throw new IllegalArgumentException("OutboxPublisher.save 参数不合法");
        }
        if (StrUtil.isBlank(event.getIdempotencyKey())) {
            event.setIdempotencyKey(event.defaultIdempotencyKey());
        }
        OutboxEvent oe = new OutboxEvent();
        oe.setIdempotencyKey(event.getIdempotencyKey());
        oe.setTopic(topic);
        oe.setTag(tag);
        oe.setBizKey(event.getBizKey());
        oe.setPayload(JSONUtil.toJsonStr(event));
        oe.setStatus(OutboxStatus.PENDING.getCode());
        oe.setRetryCount(0);
        oe.setNextRetryAt(LocalDateTime.now());

        try {
            outboxMapper.insert(oe);
            log.info("[Outbox] 事件写入 DB: id={}, idempotent={}, topic={}:{}",
                    oe.getId(), oe.getIdempotencyKey(), topic, tag);
        } catch (DuplicateKeyException dup) {
            // 幂等命中：说明同业务事件已经保存过，直接返回
            log.warn("[Outbox] 幂等命中 idempotent={}, 不重复写入", event.getIdempotencyKey());
            return outboxMapper.selectOne(new LambdaQueryWrapper<OutboxEvent>()
                    .eq(OutboxEvent::getIdempotencyKey, event.getIdempotencyKey()));
        }
        return oe;
    }

    /* ==================== XXL-Job 扫描批处理 ==================== */

    /**
     * 拉取一批待发送事件（PENDING 或到了 nextRetryAt 且未超过最大重试）
     */
    public List<OutboxEvent> pullPendingBatch(int limit) {
        return outboxMapper.selectList(new LambdaQueryWrapper<OutboxEvent>()
                .and(w -> w.eq(OutboxEvent::getStatus, OutboxStatus.PENDING.getCode())
                            .or(o -> o.eq(OutboxEvent::getStatus, OutboxStatus.FAILED.getCode())))
                .le(OutboxEvent::getNextRetryAt, LocalDateTime.now())
                .lt(OutboxEvent::getRetryCount, MAX_RETRY)
                .orderByAsc(OutboxEvent::getCreatedAt)
                .last("LIMIT " + limit));
    }

    /**
     * 发送成功：状态置为 SENT
     */
    public void markSent(OutboxEvent event) {
        outboxMapper.update(null, new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getId, event.getId())
                .set(OutboxEvent::getStatus, OutboxStatus.SENT.getCode())
                .set(OutboxEvent::getRetryCount, event.getRetryCount() + 1));
    }

    /**
     * 送达（消费端确认 ACK）：置为 DELIVERED，流程结束
     */
    public void markDelivered(String idempotencyKey) {
        outboxMapper.update(null, new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getIdempotencyKey, idempotencyKey)
                .set(OutboxEvent::getStatus, OutboxStatus.DELIVERED.getCode()));
    }

    /**
     * 发送失败：重试次数 +1，指数退避计算 nextRetryAt；超过 MAX 置为 FAILED
     */
    public void markFailed(OutboxEvent event, String errorMsg) {
        int next = event.getRetryCount() + 1;
        if (next >= MAX_RETRY) {
            log.error("[Outbox] 超过最大重试次数 idempotent={}，置为 FAILED/DEAD", event.getIdempotencyKey());
            outboxMapper.update(null, new LambdaUpdateWrapper<OutboxEvent>()
                    .eq(OutboxEvent::getId, event.getId())
                    .set(OutboxEvent::getStatus, OutboxStatus.DEAD.getCode())
                    .set(OutboxEvent::getRetryCount, next)
                    .set(OutboxEvent::getLastError, truncate(errorMsg)));
        } else {
            // 指数退避：5s -> 10s -> 20s -> 40s -> ... 最大 1h
            int backoffSeconds = Math.min((int) Math.pow(2, next) * 5, 3600);
            outboxMapper.update(null, new LambdaUpdateWrapper<OutboxEvent>()
                    .eq(OutboxEvent::getId, event.getId())
                    .set(OutboxEvent::getStatus, OutboxStatus.FAILED.getCode())
                    .set(OutboxEvent::getRetryCount, next)
                    .set(OutboxEvent::getNextRetryAt, LocalDateTime.now().plusSeconds(backoffSeconds))
                    .set(OutboxEvent::getLastError, truncate(errorMsg)));
        }
    }

    private String truncate(String msg) {
        if (StrUtil.isBlank(msg)) return null;
        return msg.length() <= 1000 ? msg : msg.substring(0, 1000);
    }
}
