package com.yingjianxia.marketing.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.marketing.entity.Promotion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动 Mapper
 */
@Mapper
public interface PromotionMapper extends BaseRepository<Promotion> {
}
