package com.yingjianxia.audit.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.audit.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseRepository<OperationLog> {
}
