package com.yingjianxia.support.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单沟通记录 — ticket_messages 表
 * <p>
 * sender_type：1用户 2客服 3系统
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_messages")
public class TicketMessage extends BaseEntity {

    /** 关联工单 */
    private Long ticketId;

    /** 1用户 2客服 3系统 */
    private Integer senderType;

    /** 发送者ID */
    private Long senderId;

    /** 发送者姓名 */
    private String senderName;

    /** 消息内容 */
    private String content;

    /** 补充附件（JSON） */
    private String images;

    /** 是否内部消息（用户不可见） */
    private Integer isInternal;

    /* ========== 本表不存在：updatedAt/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 发送者类型常量 ========== */
    public static final int SENDER_USER = 1;
    public static final int SENDER_AGENT = 2;
    public static final int SENDER_SYSTEM = 3;
}
