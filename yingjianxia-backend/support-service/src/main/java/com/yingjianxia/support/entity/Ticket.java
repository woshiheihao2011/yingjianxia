package com.yingjianxia.support.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单 — tickets 表
 * <p>
 * 状态：0待处理 → 1处理中 → 2待用户回复 → 3已解决 → 4已关闭
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tickets")
public class Ticket extends BaseEntity {

    /** 工单号 */
    private String ticketNo;

    /** 提交用户 */
    private Long userId;

    /** 关联订单 */
    private Long orderId;

    /** 问题类型 */
    private String problemType;

    /** 优先级：1高 2中 3低 */
    private Integer priority;

    /** 标题 */
    private String title;

    /** 问题描述 */
    private String description;

    /** 图片附件（JSON） */
    private String images;

    /** 联系方式 */
    private String contact;

    /** 0待处理 1处理中 2待用户回复 3已解决 4已关闭 */
    private Integer status;

    /** 分配客服ID */
    private Long agentId;

    /** 客服分组 */
    private String agentGroup;

    /** 解决时间 */
    private LocalDateTime resolvedAt;

    /** 关闭时间 */
    private LocalDateTime closedAt;

    /** 自动关闭时间（待回复7天） */
    private LocalDateTime autoCloseAt;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_WAIT_USER = 2;
    public static final int STATUS_RESOLVED = 3;
    public static final int STATUS_CLOSED = 4;
}
