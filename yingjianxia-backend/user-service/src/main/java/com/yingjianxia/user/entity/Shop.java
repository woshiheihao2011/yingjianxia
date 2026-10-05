package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 店铺实体 — shops 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shops")
public class Shop extends BaseEntity {

    /**
     * 卖家用户ID
     */
    private Long sellerId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺简介
     */
    private String description;

    /**
     * 店铺头像
     */
    private String logoUrl;

    /**
     * 封面图
     */
    private String coverUrl;

    /**
     * 店铺评分
     */
    private BigDecimal rating;

    /**
     * 粉丝数
     */
    private Integer followerCount;

    /**
     * 成交量
     */
    private Integer saleCount;

    /**
     * 在售商品数
     */
    private Integer onSaleCount;

    /**
     * 认证标记
     */
    @TableField("is_verified")
    private Boolean verified;

    /* ========== 覆盖 BaseEntity 中本表不存在的字段 ========== */

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;
}
