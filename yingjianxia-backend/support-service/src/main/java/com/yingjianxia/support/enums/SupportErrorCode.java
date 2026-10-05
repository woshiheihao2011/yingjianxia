package com.yingjianxia.support.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客服支撑服务错误码（12001~12020）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum SupportErrorCode implements IErrorCode {
    TICKET_NOT_FOUND(12001, "工单不存在"),
    TICKET_NO_PERMISSION(12002, "无权操作该工单"),
    TICKET_STATUS_INVALID(12003, "工单状态流转不合法"),
    TICKET_ALREADY_CLOSED(12004, "工单已关闭，无法继续操作"),
    TICKET_REPLY_FORBIDDEN(12005, "当前工单状态不允许回复"),
    FAQ_NOT_FOUND(12006, "FAQ不存在"),
    FAQ_DISABLED(12007, "FAQ已禁用"),
    ANNOUNCEMENT_NOT_FOUND(12008, "公告不存在"),
    ANNOUNCEMENT_DRAFT(12009, "公告为草稿状态，不可查看"),
    REPORT_NOT_FOUND(12010, "举报记录不存在"),
    REPORT_DUPLICATE(12011, "已提交过该对象的举报，请勿重复提交"),
    REPORT_ALREADY_HANDLED(12012, "举报已处理，不可重复处理"),
    QUICK_REPLY_NOT_FOUND(12013, "快捷回复配置不存在"),
    QUICK_REPLY_DISABLED(12014, "快捷回复已禁用"),
    TICKET_AGENT_NOT_ASSIGNED(12015, "工单尚未分配客服"),
    TICKET_AUTO_CLOSE_FAIL(12016, "工单自动关闭失败"),
    TARGET_TYPE_INVALID(12017, "举报目标类型不合法（1~4）"),
    REASON_TYPE_INVALID(12018, "举报原因类型不合法"),
    ANNOUNCEMENT_CATEGORY_INVALID(12019, "公告分类不合法（1~4）"),
    TICKET_TITLE_TOO_LONG(12020, "工单标题过长");

    private final Integer code;
    private final String message;
}
