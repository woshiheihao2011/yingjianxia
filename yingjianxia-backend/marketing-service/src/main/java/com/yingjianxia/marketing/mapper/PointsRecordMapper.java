package com.yingjianxia.marketing.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.marketing.entity.PointsRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分流水 Mapper（生产环境按 user_id 取模分 4 表）
 */
@Mapper
public interface PointsRecordMapper extends BaseRepository<PointsRecord> {
}
