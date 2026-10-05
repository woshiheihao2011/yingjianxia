package com.yingjianxia.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 积分流水 — points_records 表
 * <p>
 * 生产环境按 user_id 取模分 4 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("points_records")
public class PointsRecord extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 1收入 2支出 */
    private Integer type;

    /** 积分数量 */
    private Integer amount;

    /** 来源：daily_signin/browse/share/exchange/order_reward/deduction */
    private String source;

    /** 关联业务ID（订单ID/签到记录ID等） */
    private Long relatedId;

    /** 变动前可用积分 */
    private Integer balanceBefore;

    /** 变动后可用积分 */
    private Integer balanceAfter;

    /** 描述 */
    private String remark;

    /** 过期时间（收入时） */
    private LocalDateTime expireAt;

    /* ========== 本表不存在：updated_at/deleted/version ========== */
    @TableField(exist = false)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 类型常量 ========== */
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;

    /* ========== 来源常量 ========== */
    public static final String SOURCE_DAILY_SIGNIN = "daily_signin";
    public static final String SOURCE_BROWSE = "browse";
    public static final String SOURCE_SHARE = "share";
    public static final String SOURCE_ORDER_REWARD = "order_reward";
    public static final String SOURCE_DEDUCTION = "deduction";
}
