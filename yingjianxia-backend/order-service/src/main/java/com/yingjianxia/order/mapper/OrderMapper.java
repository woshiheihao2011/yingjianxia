package com.yingjianxia.order.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseRepository<Order> {
}
