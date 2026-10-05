package com.yingjianxia.product.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseRepository<Product> {
}
