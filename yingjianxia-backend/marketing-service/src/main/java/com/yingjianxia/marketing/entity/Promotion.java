package com.yingjianxia.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营销活动 — promotions 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotions")
public class Promotion extends BaseEntity {

    /** 创建卖家 */
    private Long sellerId;

    /** 所属店铺 */
    private Long shopId;

    /** 1限时折扣 2满减 3包邮 */
    private Integer type;

    /** 活动名称 */
    private String name;

    /** 折扣率（type=1） */
    private BigDecimal discountRate;

    /** 满减门槛（type=2） */
    private BigDecimal minSpend;

    /** 满减金额（type=2） */
    private BigDecimal reduceAmount;

    /** 参与活动商品ID列表（JSON） */
    private String productIds;

    /** 活动库存（限量） */
    private Integer activityStock;

    /** 已售数量 */
    private Integer soldCount;

    /** 开始时间 */
    private LocalDateTime startAt;

    /** 结束时间 */
    private LocalDateTime endAt;

    /** 0未开始 1进行中 2已结束 3已暂停 */
    private Integer status;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 类型常量 ========== */
    public static final int TYPE_FLASH_DISCOUNT = 1;
    public static final int TYPE_FULL_REDUCTION = 2;
    public static final int TYPE_FREE_SHIPPING = 3;

    /* ========== 状态常量 ========== */
    public static final int STATUS_NOT_STARTED = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_ENDED = 2;
    public static final int STATUS_PAUSED = 3;
}
