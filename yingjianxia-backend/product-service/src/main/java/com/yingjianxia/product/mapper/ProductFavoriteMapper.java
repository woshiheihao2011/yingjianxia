package com.yingjianxia.product.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.product.entity.ProductFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品收藏 Mapper
 */
@Mapper
public interface ProductFavoriteMapper extends BaseRepository<ProductFavorite> {
}
