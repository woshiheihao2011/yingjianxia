package com.yingjianxia.logistics.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.logistics.entity.LogisticsTrack;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物流轨迹 Mapper
 */
@Mapper
public interface LogisticsTrackMapper extends BaseRepository<LogisticsTrack> {
}
