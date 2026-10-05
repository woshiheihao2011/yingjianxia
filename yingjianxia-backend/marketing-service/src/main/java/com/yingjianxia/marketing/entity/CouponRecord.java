package com.yingjianxia.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 优惠券领取记录 — coupon_records 表
 * <p>
 * uk_coupon_user(coupon_id, user_id) 唯一约束保证每人每券仅一条记录（防重复领取）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_records")
public class CouponRecord extends BaseEntity {

    /** 关联优惠券 */
    private Long couponId;

    /** 领取用户 */
    private Long userId;

    /** 0未使用 1已使用 2已过期 */
    private Integer status;

    /** 使用的订单ID */
    private Long usedOrderId;

    /** 领取时间 */
    private LocalDateTime claimedAt;

    /** 使用时间 */
    private LocalDateTime usedAt;

    /** 过期时间（冗余） */
    private LocalDateTime expireAt;

    /* ========== 本表不存在：created_at/updated_at/deleted/version ========== */
    @TableField(exist = false)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_UNUSED = 0;
    public static final int STATUS_USED = 1;
    public static final int STATUS_EXPIRED = 2;
}
