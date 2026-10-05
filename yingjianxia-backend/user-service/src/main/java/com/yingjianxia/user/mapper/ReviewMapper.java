package com.yingjianxia.user.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.user.entity.Review;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评价 Mapper
 *
 * @author 硬件侠后端团队
 */
@Mapper
public interface ReviewMapper extends BaseRepository<Review> {
}
