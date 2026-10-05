package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.UserAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方登录绑定 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface UserAuthMapper extends BaseRepository<UserAuth> {
}
