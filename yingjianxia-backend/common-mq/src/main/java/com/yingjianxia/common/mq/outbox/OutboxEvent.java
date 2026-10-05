package com.yingjianxia.common.mq.outbox;

import com.yingjianxia.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Outbox Pattern — 本地消息表实体
 * <p>
 * 每个微服务需在自己的数据库中创建以下表结构：
 * <pre>
 * CREATE TABLE outbox_events (
 *   id              BIGINT       PRIMARY KEY COMMENT '雪花ID',
 *   idempotency_key VARCHAR(128) NOT NULL UNIQUE COMMENT '幂等键（事件类型+业务键）',
 *   topic           VARCHAR(128) NOT NULL COMMENT 'RocketMQ Topic',
 *   tag             VARCHAR(128) NOT NULL COMMENT 'RocketMQ Tag',
 *   biz_key         VARCHAR(128) NOT NULL COMMENT '业务键（例orderId）',
 *   payload         JSON         NOT NULL COMMENT '事件JSON序列化内容',
 *   status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待发 1已发 2送达 -1失败 -2死信',
 *   retry_count     INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
 *   next_retry_at   DATETIME     NULL COMMENT '下次重试时间',
 *   last_error      VARCHAR(1024)     NULL COMMENT '最近错误信息',
 *   created_at      DATETIME     NOT NULL,
 *   updated_at      DATETIME     DEFAULT NULL,
 *   INDEX idx_status_next(status, next_retry_at),
 *   INDEX idx_topic_tag(topic, tag)
 * ) ENGINE=InnoDB COMMENT='Outbox 本地消息表';
 * </pre>
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outbox_events")
public class OutboxEvent extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 幂等键：eventType:bizKey */
    private String idempotencyKey;

    /** MQ Topic */
    private String topic;

    /** MQ Tag */
    private String tag;

    /** 业务键 */
    private String bizKey;

    /** 事件 JSON 负载 */
    private String payload;

    /** 状态 OutboxStatus#code */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间（指数退避） */
    private java.time.LocalDateTime nextRetryAt;

    /** 最近错误消息 */
    private String lastError;
}
