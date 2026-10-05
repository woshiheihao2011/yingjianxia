package com.yingjianxia.community.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.community.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子点赞 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface PostLikeMapper extends BaseRepository<PostLike> {
}
