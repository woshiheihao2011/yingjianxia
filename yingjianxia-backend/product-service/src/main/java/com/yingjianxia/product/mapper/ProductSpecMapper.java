package com.yingjianxia.product.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.product.entity.ProductSpec;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品规格 Mapper
 */
@Mapper
public interface ProductSpecMapper extends BaseRepository<ProductSpec> {
}
