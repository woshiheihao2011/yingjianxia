package com.yingjianxia.audit.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审核服务错误码（14001~14010）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum AuditErrorCode implements IErrorCode {
    AUDIT_RECORD_NOT_FOUND(14001, "审核记录不存在"),
    NO_AUDIT_PERMISSION(14002, "无审核权限"),
    AUDIT_ALREADY_PROCESSED(14003, "该审核记录已处理，不可重复审核"),
    AUDIT_ACTION_INVALID(14004, "审核操作不合法（1通过 2拒绝 3下架）"),
    TARGET_TYPE_INVALID(14005, "审核目标类型不合法（1商品 2帖子 3举报 4提现）"),
    AUDIT_STATUS_INVALID(14006, "审核状态不合法"),
    MACHINE_AUDIT_FAIL(14007, "机审失败，转人工审核"),
    OPERATION_LOG_NOT_FOUND(14008, "操作日志不存在"),
    AUDITOR_NOT_ASSIGNED(14009, "尚未分配审核员"),
    AUDIT_TARGET_DUPLICATE(14010, "该目标已存在待审核记录，请勿重复提交");

    private final Integer code;
    private final String message;
}
