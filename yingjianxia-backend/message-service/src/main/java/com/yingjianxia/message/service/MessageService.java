package com.yingjianxia.message.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.message.dto.ConversationResp;
import com.yingjianxia.message.dto.MessageQueryResp;
import com.yingjianxia.message.dto.SendMessageReq;

/**
 * 消息服务接口（IM 私聊）
 */
public interface MessageService {

    /**
     * 发送消息（创建/复用会话 → 插消息 → 更新会话 last_message/未读数 → 幂等防重）
     */
    MessageQueryResp sendMessage(SendMessageReq req, Long senderId);

    /**
     * 消息撤回（2分钟内，仅发送者）
     */
    void recallMessage(Long messageId, Long operatorId);

    /**
     * 标记会话已读（清零当前用户未读数 + 标记消息已读）
     */
    void markRead(Long conversationId, Long userId);

    /**
     * 会话列表（按 last_message_at 排序，当前用户视角）
     */
    java.util.List<ConversationResp> listConversations(Long userId);

    /**
     * 消息历史（分页拉取，按时间正序）
     */
    PageResult<MessageQueryResp> messageHistory(Long conversationId, Long userId, Long pageNum, Long pageSize);

    /**
     * 免打扰设置
     */
    void setMute(Long conversationId, Long userId, boolean muted);

    /**
     * 通过会话发送消息（根据 conversationId 自动解析接收方）
     */
    MessageQueryResp sendMessageByConversation(Long conversationId, Long senderId, String content, Integer msgType);

    /**
     * 标记当前用户所有会话为已读
     */
    void markAllRead(Long userId);
}
