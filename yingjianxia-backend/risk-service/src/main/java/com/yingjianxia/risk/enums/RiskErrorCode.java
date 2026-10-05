package com.yingjianxia.risk.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 风控服务错误码（15001~15010）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum RiskErrorCode implements IErrorCode {
    RULE_NOT_FOUND(15001, "风控规则不存在"),
    RULE_DISABLED(15002, "风控规则已停用"),
    RISK_BLOCKED(15003, "交易已被风控拦截"),
    RISK_VERIFY_REQUIRED(15004, "触发风控验证，请完成二次验证"),
    HIGH_RISK_USER(15005, "高风险用户，操作受限"),
    RULE_TYPE_INVALID(15006, "规则类型不合法（1频率 2金额 3行为 4设备）"),
    RULE_ACTION_INVALID(15007, "处置动作不合法（1告警 2拦截 3验证）"),
    RISK_LEVEL_INVALID(15008, "风险等级不合法（1低 2中 3高）"),
    SCORE_NOT_FOUND(15009, "用户风险评分不存在"),
    RULE_DUPLICATE(15010, "规则名称已存在");

    private final Integer code;
    private final String message;
}
