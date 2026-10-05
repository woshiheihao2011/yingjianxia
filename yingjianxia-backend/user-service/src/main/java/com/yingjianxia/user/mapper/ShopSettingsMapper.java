package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.ShopSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 店铺设置 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface ShopSettingsMapper extends BaseRepository<ShopSettings> {
}
