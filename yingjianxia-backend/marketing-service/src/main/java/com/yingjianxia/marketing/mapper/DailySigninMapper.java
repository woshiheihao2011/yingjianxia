package com.yingjianxia.marketing.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.marketing.entity.DailySignin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日签到 Mapper
 */
@Mapper
public interface DailySigninMapper extends BaseRepository<DailySignin> {
}
