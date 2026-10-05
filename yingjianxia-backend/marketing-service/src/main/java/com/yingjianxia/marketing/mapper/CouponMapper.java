package com.yingjianxia.marketing.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.marketing.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券 Mapper
 */
@Mapper
public interface CouponMapper extends BaseRepository<Coupon> {
}
