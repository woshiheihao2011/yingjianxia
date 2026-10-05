package com.yingjianxia.risk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 风控记录 — risk_records 表（风控域自建表）
 * <p>
 * 风险等级 riskLevel：1低 2中 3高
 * 处置动作 action：1告警 2拦截 3验证
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("risk_records")
public class RiskRecord extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 风险类型（对应 RiskRule.ruleName） */
    private String riskType;

    /** 风险等级：1低 2中 3高 */
    private Integer riskLevel;

    /** 命中规则ID */
    private Long ruleId;

    /** 风险描述 */
    private String description;

    /** 处置动作：1告警 2拦截 3验证 */
    private Integer action;

    /** IP 地址 */
    private String ipAddress;

    /** 设备指纹ID */
    private String deviceId;

    /* ========== 本表不存在：updatedAt/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 风险等级常量 ========== */
    public static final int LEVEL_LOW = 1;
    public static final int LEVEL_MEDIUM = 2;
    public static final int LEVEL_HIGH = 3;

    /* ========== 处置动作常量 ========== */
    public static final int ACTION_WARN = 1;
    public static final int ACTION_BLOCK = 2;
    public static final int ACTION_VERIFY = 3;
}
