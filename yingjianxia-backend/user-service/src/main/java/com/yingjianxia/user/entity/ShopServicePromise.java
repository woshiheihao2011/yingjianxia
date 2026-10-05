package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务承诺实体 — shop_service_promises 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_service_promises")
public class ShopServicePromise extends BaseEntity {

    /** 关联店铺 */
    private Long shopId;

    /** 承诺编码：7day_return / fake_one_pay_three / fast_refund / shipping_insurance 等 */
    private String code;

    /** 承诺名称 */
    private String name;

    /** 是否开启 */
    private Boolean enabled;
}
