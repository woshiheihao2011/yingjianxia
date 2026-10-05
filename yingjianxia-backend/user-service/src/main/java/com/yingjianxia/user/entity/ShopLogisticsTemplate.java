package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 运费模板实体 — shop_logistics_templates 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_logistics_templates")
public class ShopLogisticsTemplate extends BaseEntity {

    /** 关联店铺 */
    private Long shopId;

    /** 模板名称 */
    private String name;

    /** 计费方式：weight(按重量)/volume(按体积)/fixed(固定运费) */
    private String type;

    /** 首件/首重运费 */
    private BigDecimal baseFee;

    /** 首件/首重数量 */
    private Integer baseUnit;

    /** 续件/续重运费 */
    private BigDecimal stepFee;

    /** 续件/续重数量 */
    private Integer stepUnit;

    /** 包邮门槛金额（满多少包邮，null 表示不包邮） */
    private BigDecimal freeShippingThreshold;

    /** 是否默认模板 */
    private Boolean isDefault;

    /** 适用地区（JSON 数组） */
    private String region;
}
