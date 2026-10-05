package com.yingjianxia.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 积分账户 — points_accounts 表
 * <p>
 * 乐观锁 version 字段（继承自 BaseEntity 的 @Version）防并发扣积分超扣。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("points_accounts")
public class PointsAccount extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 总积分 */
    private Integer totalPoints;

    /** 可用积分 */
    private Integer availablePoints;

    /** 已使用积分 */
    private Integer usedPoints;

    /** 已过期积分 */
    private Integer expiredPoints;

    /* ========== 本表不存在：created_at/deleted ========== */
    @TableField(exist = false)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Integer deleted;

    /* version 字段继承自 BaseEntity 的 @Version，本表存在该列 */
}
