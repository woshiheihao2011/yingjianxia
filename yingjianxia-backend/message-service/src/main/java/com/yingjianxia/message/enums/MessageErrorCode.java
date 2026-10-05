package com.yingjianxia.message.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息域错误码（10001~10015）
 */
@Getter
@AllArgsConstructor
public enum MessageErrorCode implements IErrorCode {
    CONVERSATION_NOT_FOUND(10001, "会话不存在"),
    MESSAGE_NOT_FOUND(10002, "消息不存在"),
    NO_PERMISSION(10003, "无权操作该消息或会话"),
    MESSAGE_ALREADY_RECALLED(10004, "消息已被撤回"),
    RECALL_TIMEOUT(10005, "消息已超过2分钟，不可撤回"),
    RECEIVER_REQUIRED(10006, "接收者不能为空"),
    CANNOT_SELF_CHAT(10007, "不能给自己发消息"),
    MESSAGE_CONTENT_EMPTY(10008, "消息内容不能为空"),
    MSG_TYPE_INVALID(10009, "消息类型无效"),
    CONVERSATION_CLOSED(10010, "会话已被对方删除"),
    CLIENT_MSG_ID_DUPLICATED(10011, "消息已发送，请勿重复提交"),
    MUTED(10012, "已设置免打扰"),
    PAGE_PARAM_INVALID(10013, "分页参数无效"),
    RECALL_NOT_SENDER(10014, "仅发送者可撤回消息"),
    MESSAGE_RECALLED_CANNOT_RECALL(10015, "消息已撤回，无法再次撤回");

    private final Integer code;
    private final String message;
}
