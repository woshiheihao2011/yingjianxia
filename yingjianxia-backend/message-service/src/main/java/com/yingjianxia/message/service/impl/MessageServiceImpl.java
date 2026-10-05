package com.yingjianxia.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.message.dto.ConversationResp;
import com.yingjianxia.message.dto.MessageQueryResp;
import com.yingjianxia.message.dto.SendMessageReq;
import com.yingjianxia.message.entity.Conversation;
import com.yingjianxia.message.entity.Message;
import com.yingjianxia.message.enums.MessageErrorCode;
import com.yingjianxia.message.mapper.ConversationMapper;
import com.yingjianxia.message.mapper.MessageMapper;
import com.yingjianxia.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息服务实现 — IM 私聊：会话复用 / 消息幂等 / 撤回 / 已读 / 免打扰
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    /** 撤回时限（分钟） */
    private static final int RECALL_WINDOW_MINUTES = 2;
    /** 最后消息预览最大长度 */
    private static final int PREVIEW_MAX_LEN = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageQueryResp sendMessage(SendMessageReq req, Long senderId) {
        // 1. 基础校验
        if (req.getReceiverId().equals(senderId)) {
            throw new BusinessException(MessageErrorCode.CANNOT_SELF_CHAT);
        }
        if (req.getMsgType() == null
                || (req.getMsgType() != Message.MSG_TYPE_TEXT && req.getMsgType() != Message.MSG_TYPE_IMAGE)) {
            throw new BusinessException(MessageErrorCode.MSG_TYPE_INVALID);
        }

        // 2. 幂等防重：client_msg_id + sender_id
        Message exist = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getClientMsgId, req.getClientMsgId())
                .eq(Message::getSenderId, senderId)
                .last("LIMIT 1"));
        if (exist != null) {
            log.info("【消息幂等】clientMsgId={} 已存在，返回原消息 id={}", req.getClientMsgId(), exist.getId());
            return toResp(exist);
        }

        // 3. 复用或创建会话（A=较小ID，B=较大ID）
        long a = Math.min(senderId, req.getReceiverId());
        long b = Math.max(senderId, req.getReceiverId());
        Conversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserAId, a)
                .eq(Conversation::getUserBId, b)
                .eq(req.getProductId() != null, Conversation::getProductId, req.getProductId())
                .last("LIMIT 1"));
        boolean newConv = false;
        if (conv == null) {
            conv = new Conversation();
            conv.setUserAId(a);
            conv.setUserBId(b);
            conv.setProductId(req.getProductId());
            conv.setUnreadA(0);
            conv.setUnreadB(0);
            conv.setAIsMuted(false);
            conv.setBIsMuted(false);
            conv.setAStatus(Conversation.STATUS_NORMAL);
            conv.setBStatus(Conversation.STATUS_NORMAL);
            conversationMapper.insert(conv);
            newConv = true;
        }

        // 4. 插入消息
        Message m = new Message();
        m.setConversationId(conv.getId());
        m.setSenderId(senderId);
        m.setReceiverId(req.getReceiverId());
        m.setMsgType(req.getMsgType());
        m.setContent(req.getContent());
        m.setIsRead(false);
        m.setStatus(Message.STATUS_NORMAL);
        m.setClientMsgId(req.getClientMsgId());
        messageMapper.insert(m);

        // 5. 更新会话：last_message / last_message_id / last_message_at / 接收方未读数
        LocalDateTime now = LocalDateTime.now();
        String preview = preview(req.getMsgType(), req.getContent());
        boolean senderIsA = (senderId == a);
        LambdaUpdateWrapper<Conversation> uw = new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conv.getId())
                .set(Conversation::getLastMessage, preview)
                .set(Conversation::getLastMessageId, m.getId())
                .set(Conversation::getLastMessageAt, now)
                .set(Conversation::getUpdatedAt, now);
        // 若会话曾被对方删除，重新激活双方状态
        if (newConv || conv.getAStatus() == null) {
            uw.set(Conversation::getAStatus, Conversation.STATUS_NORMAL);
        }
        if (newConv || conv.getBStatus() == null) {
            uw.set(Conversation::getBStatus, Conversation.STATUS_NORMAL);
        }
        // 接收方未读 +1
        if (senderIsA) {
            uw.setSql("unread_b = unread_b + 1");
        } else {
            uw.setSql("unread_a = unread_a + 1");
        }
        conversationMapper.update(null, uw);

        log.info("【发送消息】conv={}, msgId={}, sender={}, receiver={}, type={}",
                conv.getId(), m.getId(), senderId, req.getReceiverId(), req.getMsgType());
        return toResp(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId, Long operatorId) {
        Message m = requireMessage(messageId);
        if (!m.getSenderId().equals(operatorId)) {
            throw new BusinessException(MessageErrorCode.RECALL_NOT_SENDER);
        }
        if (m.getStatus() == Message.STATUS_RECALLED) {
            throw new BusinessException(MessageErrorCode.MESSAGE_ALREADY_RECALLED);
        }
        // 2 分钟内可撤回
        if (m.getCreatedAt() != null
                && Duration.between(m.getCreatedAt(), LocalDateTime.now()).toMinutes() > RECALL_WINDOW_MINUTES) {
            throw new BusinessException(MessageErrorCode.RECALL_TIMEOUT);
        }
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, messageId)
                .set(Message::getStatus, Message.STATUS_RECALLED)
                .set(Message::getContent, "该消息已被撤回"));
        log.info("【撤回消息】msgId={}, operator={}", messageId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long conversationId, Long userId) {
        Conversation conv = requireConversation(conversationId);
        ensureParticipant(conv, userId);
        LocalDateTime now = LocalDateTime.now();
        // 1. 清零当前用户未读数
        boolean isA = conv.getUserAId().equals(userId);
        LambdaUpdateWrapper<Conversation> uw = new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getUpdatedAt, now);
        if (isA) {
            uw.set(Conversation::getUnreadA, 0);
        } else {
            uw.set(Conversation::getUnreadB, 0);
        }
        conversationMapper.update(null, uw);
        // 2. 标记该会话中发给当前用户的未读消息为已读
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, false)
                .set(Message::getIsRead, true)
                .set(Message::getReadAt, now));
    }

    @Override
    public List<ConversationResp> listConversations(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUserAId, userId).eq(Conversation::getAStatus, Conversation.STATUS_NORMAL)
                        .or().eq(Conversation::getUserBId, userId).eq(Conversation::getBStatus, Conversation.STATUS_NORMAL))
                .orderByDesc(Conversation::getLastMessageAt));
        List<ConversationResp> list = new ArrayList<>(convs.size());
        for (Conversation c : convs) {
            list.add(toResp(c, userId));
        }
        return list;
    }

    @Override
    public PageResult<MessageQueryResp> messageHistory(Long conversationId, Long userId, Long pageNum, Long pageSize) {
        Conversation conv = requireConversation(conversationId);
        ensureParticipant(conv, userId);
        if (pageNum == null || pageNum < 1) pageNum = 1L;
        if (pageSize == null || pageSize < 1) pageSize = 20L;

        Page<Message> page = new Page<>(pageNum, pageSize);
        Page<Message> result = messageMapper.selectPage(page, new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreatedAt));
        // 反转为正序（前端通常按时间正序展示，倒序拉取最新页后反转）
        List<MessageQueryResp> records = new ArrayList<>(result.getRecords().size());
        for (int i = result.getRecords().size() - 1; i >= 0; i--) {
            records.add(toResp(result.getRecords().get(i)));
        }
        return PageResult.of(pageNum, pageSize, result.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMute(Long conversationId, Long userId, boolean muted) {
        Conversation conv = requireConversation(conversationId);
        ensureParticipant(conv, userId);
        boolean isA = conv.getUserAId().equals(userId);
        LambdaUpdateWrapper<Conversation> uw = new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getUpdatedAt, LocalDateTime.now());
        if (isA) {
            uw.set(Conversation::getAIsMuted, muted);
        } else {
            uw.set(Conversation::getBIsMuted, muted);
        }
        conversationMapper.update(null, uw);
        log.info("【免打扰设置】conv={}, user={}, muted={}", conversationId, userId, muted);
    }

    @Override
    public MessageQueryResp sendMessageByConversation(Long conversationId, Long senderId, String content, Integer msgType) {
        Conversation conv = requireConversation(conversationId);
        ensureParticipant(conv, senderId);
        Long receiverId = conv.getUserAId().equals(senderId) ? conv.getUserBId() : conv.getUserAId();
        SendMessageReq req = new SendMessageReq();
        req.setReceiverId(receiverId);
        req.setProductId(conv.getProductId());
        req.setMsgType(msgType != null ? msgType : Message.MSG_TYPE_TEXT);
        req.setContent(content);
        req.setClientMsgId(java.util.UUID.randomUUID().toString());
        return sendMessage(req, senderId);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUserAId, userId).or().eq(Conversation::getUserBId, userId)));
        LocalDateTime now = LocalDateTime.now();
        for (Conversation conv : convs) {
            boolean isA = conv.getUserAId().equals(userId);
            LambdaUpdateWrapper<Conversation> uw = new LambdaUpdateWrapper<Conversation>()
                    .eq(Conversation::getId, conv.getId())
                    .set(Conversation::getUpdatedAt, now);
            if (isA) {
                uw.set(Conversation::getUnreadA, 0);
            } else {
                uw.set(Conversation::getUnreadB, 0);
            }
            conversationMapper.update(null, uw);
        }
        // 标记所有发给当前用户的未读消息为已读
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, false)
                .set(Message::getIsRead, true)
                .set(Message::getReadAt, now));
        log.info("【全部已读】userId={}, convCount={}", userId, convs.size());
    }

    /* ======================== 内部工具 ======================== */

    private Conversation requireConversation(Long id) {
        Conversation c = conversationMapper.selectById(id);
        if (c == null) throw new BusinessException(MessageErrorCode.CONVERSATION_NOT_FOUND);
        return c;
    }

    private Message requireMessage(Long id) {
        Message m = messageMapper.selectById(id);
        if (m == null) throw new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND);
        return m;
    }

    private void ensureParticipant(Conversation conv, Long userId) {
        if (!conv.getUserAId().equals(userId) && !conv.getUserBId().equals(userId)) {
            throw new BusinessException(MessageErrorCode.NO_PERMISSION);
        }
    }

    private MessageQueryResp toResp(Message m) {
        MessageQueryResp r = new MessageQueryResp();
        r.setId(m.getId());
        r.setConversationId(m.getConversationId());
        r.setSenderId(m.getSenderId());
        r.setReceiverId(m.getReceiverId());
        r.setMsgType(m.getMsgType());
        r.setContent(m.getContent());
        r.setIsRead(m.getIsRead());
        r.setStatus(m.getStatus());
        r.setClientMsgId(m.getClientMsgId());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }

    private ConversationResp toResp(Conversation c, Long currentUserId) {
        ConversationResp r = new ConversationResp();
        r.setId(c.getId());
        boolean isA = c.getUserAId().equals(currentUserId);
        r.setPeerId(isA ? c.getUserBId() : c.getUserAId());
        r.setProductId(c.getProductId());
        r.setLastMessage(c.getLastMessage());
        r.setLastMessageId(c.getLastMessageId());
        r.setLastMessageAt(c.getLastMessageAt());
        r.setUnreadCount(isA ? (c.getUnreadA() == null ? 0 : c.getUnreadA())
                : (c.getUnreadB() == null ? 0 : c.getUnreadB()));
        r.setMuted(isA ? Boolean.TRUE.equals(c.getAIsMuted()) : Boolean.TRUE.equals(c.getBIsMuted()));
        return r;
    }

    /**
     * 生成消息预览：图片显示 [图片]，文本截断
     */
    private String preview(int msgType, String content) {
        if (msgType == Message.MSG_TYPE_IMAGE) return "[图片]";
        if (StrUtil.isBlank(content)) return "";
        return content.length() > PREVIEW_MAX_LEN ? content.substring(0, PREVIEW_MAX_LEN) + "..." : content;
    }
}
