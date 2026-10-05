package com.yingjianxia.product.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.product.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper
 */
@Mapper
public interface CategoryMapper extends BaseRepository<Category> {
}
