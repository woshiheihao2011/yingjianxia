package com.yingjianxia.common.mq.tx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RocketMQ 事务消息发送模板
 * <p>
 * 使用方式：
 * <pre>
 * txTemplate.sendMessageInTransaction(
 *     "ORDER_EVENTS_TOPIC:OrderCreated",   // topic:tag
 *     orderCreatedEvent,                    // payload
 *     args -> {                             // 本地事务执行逻辑
 *         // 1. 创建订单
 *         // 2. 预扣库存
 *         // 3. 冻结资金
 *         return RocketMqLocalTxState.COMMIT;
 *     }
 * );
 * </pre>
 * <p>
 * 流程：
 * <ol>
 *   <li>发送半消息（Half Message）到 Broker</li>
 *   <li>Broker 返回成功 → 执行本地事务回调</li>
 *   <li>本地事务成功 → COMMIT 半消息 → 下游消费者可消费</li>
 *   <li>本地事务失败 → ROLLBACK，半消息丢弃</li>
 *   <li>超时未知 → Broker 回查本地事务状态</li>
 * </ol>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@ConditionalOnBean(RocketMQTemplate.class)
@Component
@RequiredArgsConstructor
public class RocketMqTransactionTemplate {

    private final RocketMQTemplate rocketTemplate;

    /**
     * 发送事务消息（默认 destination = topic:tag，事务 ID 自动生成）
     *
     * @param destination  topic:tag
     * @param payload      业务负载
     * @param txCallback   本地事务回调
     * @param <P>          负载类型
     */
    public <P> void send(String destination, P payload, RocketMqLocalTxCallback txCallback) {
        String txId = UUID.randomUUID().toString().replace("-", "");
        Message<P> msg = MessageBuilder.withPayload(payload)
                .setHeader(RocketMQHeaders.TRANSACTION_ID, txId)
                .setHeader(RocketMQHeaders.KEYS, txId)
                .build();

        try {
            // NOTE: 真实使用时需要一个 Spring 管理的 TransactionListener 实现类，
            // 用 txId → callback 的注册表做路由。此处为简化的模板封装，
            // 各微服务实现 @RocketMQTransactionListener 时注入的执行器复用此类的工具方法。
            log.info("[RocketMQ-TX] 发送事务半消息 txId={}, destination={}", txId, destination);
            rocketTemplate.sendMessageInTransaction(destination, msg, txCallback);
        } catch (Exception e) {
            log.error("[RocketMQ-TX] 发送事务消息失败 txId={}, err={}", txId, e.getMessage());
            throw new RuntimeException("事务消息发送失败", e);
        }
    }
}
