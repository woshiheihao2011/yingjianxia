package com.yingjianxia.community.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.community.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子评论 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface PostCommentMapper extends BaseRepository<PostComment> {
}
