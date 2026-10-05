package com.yingjianxia.logistics.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.logistics.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发货记录 Mapper
 */
@Mapper
public interface ShipmentMapper extends BaseRepository<Shipment> {
}
