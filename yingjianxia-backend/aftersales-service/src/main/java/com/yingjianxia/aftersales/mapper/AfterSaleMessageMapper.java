package com.yingjianxia.aftersales.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.aftersales.entity.AfterSaleMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后沟通记录 Mapper
 */
@Mapper
public interface AfterSaleMessageMapper extends BaseRepository<AfterSaleMessage> {
}
