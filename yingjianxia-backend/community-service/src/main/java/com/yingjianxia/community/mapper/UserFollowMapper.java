package com.yingjianxia.community.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.community.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户关注 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface UserFollowMapper extends BaseRepository<UserFollow> {
}
