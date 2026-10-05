package com.yingjianxia.payment.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付订单 Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseRepository<PaymentOrder> {
}
