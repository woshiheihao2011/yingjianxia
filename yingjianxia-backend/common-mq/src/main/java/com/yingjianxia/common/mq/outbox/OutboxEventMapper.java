package com.yingjianxia.common.mq.outbox;

import com.yingjianxia.common.mybatis.base.BaseRepository;

/**
 * Outbox 事件 Mapper 基类
 * <p>
 * 每个微服务继承此接口，且确保 MapperScan 能扫描到。
 * 示例：
 * <pre>
 * &#64;Mapper
 * public interface OrderOutboxMapper extends OutboxEventMapper { }
 * </pre>
 *
 * @author 硬件侠后端团队
 */
public interface OutboxEventMapper extends BaseRepository<OutboxEvent> {
}
