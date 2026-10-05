package com.yingjianxia.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息表 — messages
 * <p>
 * 注：生产环境按 conversation_id 取模分 16 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("messages")
public class Message extends BaseEntity {

    /** 关联会话 */
    private Long conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 消息类型：1文本 2图片 */
    private Integer msgType;

    /** 消息内容/图片URL */
    private String content;

    /** 是否已读 */
    private Boolean isRead;

    /** 已读时间 */
    private LocalDateTime readAt;

    /** 1正常 2撤回 */
    private Integer status;

    /** 客户端消息ID（幂等用） */
    private String clientMsgId;

    /* ========== 本表不存在：deleted/version/updated_at ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;
    @TableField(exist = false) private LocalDateTime updatedAt;

    /* ========== 常量 ========== */
    public static final int MSG_TYPE_TEXT = 1;
    public static final int MSG_TYPE_IMAGE = 2;

    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_RECALLED = 2;
}
