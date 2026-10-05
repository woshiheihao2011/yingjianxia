package com.yingjianxia.aftersales.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.aftersales.entity.AfterSaleStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后状态变更日志 Mapper
 */
@Mapper
public interface AfterSaleStatusLogMapper extends BaseRepository<AfterSaleStatusLog> {
}
