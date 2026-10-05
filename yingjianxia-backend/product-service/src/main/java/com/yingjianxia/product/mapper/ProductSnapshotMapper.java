package com.yingjianxia.product.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.product.entity.ProductSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品快照 Mapper
 */
@Mapper
public interface ProductSnapshotMapper extends BaseRepository<ProductSnapshot> {
}
