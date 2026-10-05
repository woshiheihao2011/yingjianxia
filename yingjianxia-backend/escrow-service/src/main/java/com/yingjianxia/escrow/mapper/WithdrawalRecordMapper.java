package com.yingjianxia.escrow.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.escrow.entity.WithdrawalRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WithdrawalRecordMapper extends BaseRepository<WithdrawalRecord> {
}
