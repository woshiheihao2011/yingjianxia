package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.UserRealName;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实名认证 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface UserRealNameMapper extends BaseRepository<UserRealName> {
}
