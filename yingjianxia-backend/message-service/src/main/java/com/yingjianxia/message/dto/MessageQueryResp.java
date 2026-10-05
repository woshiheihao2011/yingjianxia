package com.yingjianxia.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息查询响应
 */
@Data
@Schema(description = "消息响应")
public class MessageQueryResp {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "发送者ID")
    private Long senderId;

    @Schema(description = "接收者ID")
    private Long receiverId;

    @Schema(description = "消息类型：1文本 2图片")
    private Integer msgType;

    @Schema(description = "消息内容/图片URL")
    private String content;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "状态：1正常 2撤回")
    private Integer status;

    @Schema(description = "客户端消息ID")
    private String clientMsgId;

    @Schema(description = "发送时间")
    private LocalDateTime createdAt;
}
