package com.yingjianxia.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券 — coupons 表
 * <p>
 * 防超发核心：claimed_count 通过原子 UPDATE（WHERE claimed_count &lt; total_count）扣减。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupons")
public class Coupon extends BaseEntity {

    /** 创建者（卖家ID） */
    private Long creatorId;

    /** 所属店铺 */
    private Long shopId;

    /** 券名称 */
    private String name;

    /** 1满减券 2折扣券 3包邮券 */
    private Integer couponType;

    /** 面额（满减券使用） */
    private BigDecimal faceValue;

    /** 折扣率（折扣券使用，如0.85=8.5折） */
    private BigDecimal discountRate;

    /** 使用门槛（满X可用） */
    private BigDecimal minSpend;

    /** 发放总量 */
    private Integer totalCount;

    /** 已领取数 */
    private Integer claimedCount;

    /** 已使用数 */
    private Integer usedCount;

    /** 每人限领 */
    private Integer perUserLimit;

    /** 适用范围：0全店 1指定品类 2指定商品 */
    private Integer scope;

    /** 适用范围值（品类ID/商品ID JSON） */
    private String scopeValue;

    /** 有效期开始 */
    private LocalDateTime validStart;

    /** 有效期结束 */
    private LocalDateTime validEnd;

    /** 1进行中 2已暂停 3已结束 */
    private Integer status;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 类型常量 ========== */
    public static final int TYPE_FULL_REDUCTION = 1;
    public static final int TYPE_DISCOUNT = 2;
    public static final int TYPE_FREE_SHIPPING = 3;

    /* ========== 状态常量 ========== */
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_ENDED = 3;
}
