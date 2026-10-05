package com.yingjianxia.risk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户风险评分 — user_risk_scores 表（风控域自建表）
 * <p>
 * 风险等级 level：1低 2中 3高
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_risk_scores")
public class UserRiskScore extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 风险评分（0~100，越高风险越大） */
    private Integer score;

    /** 风险等级：1低 2中 3高 */
    private Integer level;

    /** 最后更新时间 */
    private LocalDateTime lastUpdate;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 风险等级常量 ========== */
    public static final int LEVEL_LOW = 1;
    public static final int LEVEL_MEDIUM = 2;
    public static final int LEVEL_HIGH = 3;

    /** 评分阈值：>=60 中风险，>=80 高风险 */
    public static final int THRESHOLD_MEDIUM = 60;
    public static final int THRESHOLD_HIGH = 80;
}
