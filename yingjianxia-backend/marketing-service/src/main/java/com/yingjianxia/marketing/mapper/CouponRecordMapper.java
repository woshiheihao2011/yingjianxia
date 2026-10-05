package com.yingjianxia.marketing.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.marketing.entity.CouponRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取记录 Mapper
 */
@Mapper
public interface CouponRecordMapper extends BaseRepository<CouponRecord> {
}
