package com.yingjianxia.common.mq.outbox;

/**
 * Outbox 事件状态枚举
 *
 * @author 硬件侠后端团队
 */
public enum OutboxStatus {

    /** 待发送（初始） */
    PENDING(0),

    /** 已发送（等待消费确认） */
    SENT(1),

    /** 已成功送达（消费端 ACK 后置此状态） */
    DELIVERED(2),

    /** 发送失败（超过最大重试次数） */
    FAILED(-1),

    /** 死信（人工处理） */
    DEAD(-2);

    private final int code;

    OutboxStatus(int code) { this.code = code; }

    public int getCode() { return code; }

    public static OutboxStatus of(int code) {
        for (OutboxStatus s : values()) {
            if (s.code == code) return s;
        }
        return PENDING;
    }
}
