package com.yingjianxia.message.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseRepository<Conversation> {
}
