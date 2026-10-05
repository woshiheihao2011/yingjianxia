package com.yingjianxia.risk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 风控规则 — risk_rules 表（风控域自建表）
 * <p>
 * 规则类型 ruleType：1频率 2金额 3行为 4设备
 * 处置动作 action：1告警 2拦截 3验证
 * 状态 status：1启用 0停用
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("risk_rules")
public class RiskRule extends BaseEntity {

    /** 规则名称 */
    private String ruleName;

    /** 规则类型：1频率 2金额 3行为 4设备 */
    private Integer ruleType;

    /** 规则配置（JSON） */
    private String ruleConfig;

    /** 处置动作：1告警 2拦截 3验证 */
    private Integer action;

    /** 1启用 0停用 */
    private Integer status;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 规则类型常量 ========== */
    public static final int TYPE_FREQUENCY = 1;
    public static final int TYPE_AMOUNT = 2;
    public static final int TYPE_BEHAVIOR = 3;
    public static final int TYPE_DEVICE = 4;

    /* ========== 处置动作常量 ========== */
    public static final int ACTION_WARN = 1;
    public static final int ACTION_BLOCK = 2;
    public static final int ACTION_VERIFY = 3;

    /* ========== 状态常量 ========== */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}
