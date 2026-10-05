package com.yingjianxia.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表响应（当前用户视角）
 */
@Data
@Schema(description = "会话响应")
public class ConversationResp {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "对方用户ID")
    private Long peerId;

    @Schema(description = "关联商品ID")
    private Long productId;

    @Schema(description = "最后消息预览")
    private String lastMessage;

    @Schema(description = "最后消息ID")
    private Long lastMessageId;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "当前用户未读数")
    private Integer unreadCount;

    @Schema(description = "当前用户是否免打扰")
    private Boolean muted;
}
