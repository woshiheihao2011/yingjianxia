package com.yingjianxia.common.core.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件基类 — 所有 RocketMQ 事件消息必须继承
 * <p>
 * 统一提供事件追踪能力：eventId、业务键、触发时间、幂等键、来源服务。
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Data
public abstract class BaseDomainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一ID（UUID 去重）
     */
    private String eventId;

    /**
     * 事件业务键（例：orderId / productId / userId）
     */
    private String bizKey;

    /**
     * 事件类型（类名，反序列化/审计日志用）
     */
    private String eventType;

    /**
     * 事件发生时间戳（ms）
     */
    private Long eventTime;

    /**
     * 事件幂等键（消费端去重用）
     */
    private String idempotencyKey;

    /**
     * 来源服务名（例：user-service）
     */
    private String sourceService;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 重放次数（首次 = 0）
     */
    private Integer retryCount = 0;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString().replace("-", "");
        this.eventTime = Instant.now().toEpochMilli();
        this.eventType = this.getClass().getSimpleName();
        this.sourceService = getServiceName();
    }

    /**
     * 子类覆盖：返回服务名，用于审计
     */
    protected abstract String getServiceName();

    /**
     * 构造通用幂等键：eventType:bizKey
     */
    public String defaultIdempotencyKey() {
        return this.eventType + ":" + this.bizKey;
    }
}
