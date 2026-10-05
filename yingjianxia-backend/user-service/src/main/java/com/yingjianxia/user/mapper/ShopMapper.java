package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

/**
 * 店铺 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface ShopMapper extends BaseRepository<Shop> {
}
