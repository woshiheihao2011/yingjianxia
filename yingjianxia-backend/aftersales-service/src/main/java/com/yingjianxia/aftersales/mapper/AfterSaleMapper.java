package com.yingjianxia.aftersales.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.aftersales.entity.AfterSale;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后申请 Mapper
 */
@Mapper
public interface AfterSaleMapper extends BaseRepository<AfterSale> {
}
