package com.yingjianxia.common.mq.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Outbox 事件发送执行器
 * <p>
 * XXL-Job 定时扫描 {@link OutboxPublisherTemplate#pullPendingBatch} 后，
 * 调用此类的 {@link #sendOne(OutboxEvent)} 实际发送 RocketMQ 普通消息。
 * 对于需要强一致性的场景（如订单TCC），建议使用
 * {@link com.yingjianxia.common.mq.tx.RocketMqTransactionTemplate} 发送事务消息。
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@ConditionalOnBean(RocketMQTemplate.class)
@Component
@RequiredArgsConstructor
public class OutboxRocketMqSender {

    private final RocketMQTemplate rocketTemplate;

    /**
     * 单条发送：destination = {@code topic:tag}
     */
    public boolean sendOne(OutboxEvent event) {
        String destination = event.getTopic() + ":" + (event.getTag() == null ? "*" : event.getTag());
        try {
            Message<String> msg = MessageBuilder.withPayload(event.getPayload())
                    .setHeader("KEYS", event.getIdempotencyKey())
                    .setHeader("EVENT_ID", event.getId())
                    .setHeader("IDEMPOTENT", event.getIdempotencyKey())
                    .build();
            rocketTemplate.sendOneWay(destination, msg);
            log.info("[Outbox] MQ 发送成功 idempotent={}, destination={}", event.getIdempotencyKey(), destination);
            return true;
        } catch (Exception e) {
            log.error("[Outbox] MQ 发送失败 idempotent={}, err={}", event.getIdempotencyKey(), e.getMessage());
            return false;
        }
    }
}
