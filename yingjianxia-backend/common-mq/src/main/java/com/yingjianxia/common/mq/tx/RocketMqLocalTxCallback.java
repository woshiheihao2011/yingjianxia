package com.yingjianxia.common.mq.tx;

/**
 * RocketMQ 本地事务回调
 * <p>
 * 执行本地业务逻辑，根据结果返回 COMMIT / ROLLBACK / UNKNOWN。
 *
 * @author 硬件侠后端团队
 */
@FunctionalInterface
public interface RocketMqLocalTxCallback {

    /**
     * 执行本地事务
     *
     * @param msgPayload 消息负载（原始对象）
     * @param arg        附加参数
     */
    RocketMqLocalTxState execute(Object msgPayload, Object arg);
}
