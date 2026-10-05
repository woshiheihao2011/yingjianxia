package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface AddressMapper extends BaseRepository<Address> {
}
