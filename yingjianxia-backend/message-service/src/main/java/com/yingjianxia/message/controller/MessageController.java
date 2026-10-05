package com.yingjianxia.message.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.message.dto.ConversationResp;
import com.yingjianxia.message.dto.MessageQueryResp;
import com.yingjianxia.message.dto.SendMessageReq;
import com.yingjianxia.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息服务 Controller（IM 私聊）
 */
@Tag(name = "消息服务", description = "发送消息/撤回/已读/会话列表/消息历史/免打扰")
@RestController
@RequestMapping({"/api/v1/messages", "/api/v1/message"})
@RequiredArgsConstructor
public class MessageController {

    private final MessageService service;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public ApiResponse<MessageQueryResp> send(@Valid @RequestBody SendMessageReq req) {
        return ApiResponse.success(service.sendMessage(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "撤回消息（2分钟内）")
    @PostMapping("/messages/{messageId}/recall")
    public ApiResponse<Void> recall(@PathVariable Long messageId) {
        service.recallMessage(messageId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "标记会话已读")
    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long conversationId) {
        service.markRead(conversationId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "通过会话发送消息")
    @PostMapping("/conversations/{conversationId}")
    public ApiResponse<MessageQueryResp> sendByConversation(@PathVariable Long conversationId,
                                                             @RequestBody java.util.Map<String, Object> body) {
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));
        String type = body.get("type") == null ? "text" : String.valueOf(body.get("type"));
        int msgType = "image".equalsIgnoreCase(type)
                ? com.yingjianxia.message.entity.Message.MSG_TYPE_IMAGE
                : com.yingjianxia.message.entity.Message.MSG_TYPE_TEXT;
        return ApiResponse.success(service.sendMessageByConversation(conversationId, UserContext.requiredUserId(), content, msgType));
    }

    @Operation(summary = "全部消息已读")
    @PostMapping("/read-all")
    public ApiResponse<Void> readAll() {
        service.markAllRead(UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "会话列表（按最后消息时间倒序）")
    @GetMapping({"", "/conversations"})
    public ApiResponse<List<ConversationResp>> conversations() {
        return ApiResponse.success(service.listConversations(UserContext.requiredUserId()));
    }

    @Operation(summary = "未读消息总数")
    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        Long userId = UserContext.requiredUserId();
        List<ConversationResp> convos = service.listConversations(userId);
        int total = 0;
        if (convos != null) {
            for (ConversationResp c : convos) {
                if (c.getUnreadCount() != null) total += c.getUnreadCount();
            }
        }
        return ApiResponse.success(total);
    }

    @Operation(summary = "消息历史（分页拉取，时间正序）")
    @GetMapping({"/conversations/{conversationId}/messages", "/conversations/{conversationId}"})
    public ApiResponse<PageResult<MessageQueryResp>> history(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.success(service.messageHistory(conversationId, UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "免打扰设置")
    @PostMapping("/conversations/{conversationId}/mute")
    public ApiResponse<Void> setMute(@PathVariable Long conversationId,
                                     @RequestParam boolean muted) {
        service.setMute(conversationId, UserContext.requiredUserId(), muted);
        return ApiResponse.success();
    }
}
