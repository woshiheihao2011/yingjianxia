package com.yingjianxia.common.mq.tx;

/**
 * RocketMQ 本地事务状态
 * <ul>
 *   <li>COMMIT ：提交，半消息变为可投递 → 下游消费者收到</li>
 *   <li>ROLLBACK：回滚，半消息丢弃，不触发下游</li>
 *   <li>UNKNOWN ：未知，等待 Broker 回查</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
public enum RocketMqLocalTxState {
    COMMIT,
    ROLLBACK,
    UNKNOWN
}
