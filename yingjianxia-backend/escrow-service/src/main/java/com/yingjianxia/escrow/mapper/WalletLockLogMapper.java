package com.yingjianxia.escrow.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.escrow.entity.WalletLockLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletLockLogMapper extends BaseRepository<WalletLockLog> {
}
