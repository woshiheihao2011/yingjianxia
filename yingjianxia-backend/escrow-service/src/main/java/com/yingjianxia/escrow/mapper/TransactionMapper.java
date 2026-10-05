package com.yingjianxia.escrow.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.escrow.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper extends BaseRepository<Transaction> {
}
