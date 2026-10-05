package com.yingjianxia.community.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.community.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface CommunityPostMapper extends BaseRepository<CommunityPost> {
}
