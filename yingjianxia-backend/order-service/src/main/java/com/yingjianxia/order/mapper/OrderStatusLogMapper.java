package com.yingjianxia.order.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.order.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStatusLogMapper extends BaseRepository<OrderStatusLog> {
}
