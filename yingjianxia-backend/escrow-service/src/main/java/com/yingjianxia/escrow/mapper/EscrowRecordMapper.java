package com.yingjianxia.escrow.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.escrow.entity.EscrowRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EscrowRecordMapper extends BaseRepository<EscrowRecord> {
}
