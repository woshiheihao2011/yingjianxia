package com.yingjianxia.message.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseRepository<Message> {
}
