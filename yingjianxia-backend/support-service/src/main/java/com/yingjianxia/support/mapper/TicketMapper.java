package com.yingjianxia.support.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.support.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketMapper extends BaseRepository<Ticket> {
}
