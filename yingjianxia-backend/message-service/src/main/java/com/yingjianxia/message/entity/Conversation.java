package com.yingjianxia.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话表 — conversations
 * <p>
 * 约定：user_a_id 取较小ID方，user_b_id 取较大ID方，便于唯一约束。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversations")
public class Conversation extends BaseEntity {

    /** 用户A（较小ID方） */
    private Long userAId;

    /** 用户B（较大ID方） */
    private Long userBId;

    /** 关联商品（聊天头部嵌入商品卡片） */
    private Long productId;

    /** 最后消息预览 */
    private String lastMessage;

    /** 最后消息ID */
    private Long lastMessageId;

    /** 最后消息时间 */
    private LocalDateTime lastMessageAt;

    /** A未读数 */
    private Integer unreadA;

    /** B未读数 */
    private Integer unreadB;

    /** A是否免打扰 */
    private Boolean aIsMuted;

    /** B是否免打扰 */
    private Boolean bIsMuted;

    /** A状态：1正常 2已删除 */
    private Integer aStatus;

    /** B状态：1正常 2已删除 */
    private Integer bStatus;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 常量 ========== */
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 2;
}
