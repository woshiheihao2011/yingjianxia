package com.yingjianxia.audit.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.audit.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditRecordMapper extends BaseRepository<AuditRecord> {
}
