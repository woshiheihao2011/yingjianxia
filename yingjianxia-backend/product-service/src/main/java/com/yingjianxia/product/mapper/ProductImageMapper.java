package com.yingjianxia.product.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.product.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品图片 Mapper
 */
@Mapper
public interface ProductImageMapper extends BaseRepository<ProductImage> {
}
