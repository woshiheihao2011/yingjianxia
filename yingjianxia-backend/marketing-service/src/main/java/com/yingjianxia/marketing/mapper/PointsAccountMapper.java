package com.yingjianxia.marketing.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.marketing.entity.PointsAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分账户 Mapper（乐观锁 version 字段继承自 BaseEntity）
 */
@Mapper
public interface PointsAccountMapper extends BaseRepository<PointsAccount> {
}
