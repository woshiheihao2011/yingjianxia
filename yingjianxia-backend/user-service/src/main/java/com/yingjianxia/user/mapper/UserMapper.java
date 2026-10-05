package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface UserMapper extends BaseRepository<User> {
}
