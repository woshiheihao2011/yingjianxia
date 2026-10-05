package com.yingjianxia.order.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseRepository<OrderItem> {
}
