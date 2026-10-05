package com.yingjianxia.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到 — daily_signins 表
 * <p>
 * uk_user_date(user_id, signin_date) 唯一约束保证每日仅签一次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_signins")
public class DailySignin extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 签到日期 */
    private LocalDate signinDate;

    /** 获得积分 */
    private Integer pointsEarned;

    /** 连续签到天数 */
    private Integer continuousDays;

    /* ========== 本表不存在：updated_at/deleted/version ========== */
    @TableField(exist = false)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /** 每日签到基础积分 */
    public static final int BASE_POINTS = 10;
}
