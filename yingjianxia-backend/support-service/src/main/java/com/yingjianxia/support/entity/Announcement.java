package com.yingjianxia.support.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告 — announcements 表
 * <p>
 * 分类：1平台公告 2活动通知 3政策更新 4系统维护
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcements")
public class Announcement extends BaseEntity {

    /** 标题 */
    private String title;

    /** 1平台公告 2活动通知 3政策更新 4系统维护 */
    private Integer category;

    /** 正文 */
    private String content;

    /** 附件列表（JSON） */
    private String attachments;

    /** 是否置顶 */
    private Integer isPinned;

    /** 阅读量 */
    private Integer viewCount;

    /** 1已发布 0草稿 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_DRAFT = 0;

    /* ========== 分类常量 ========== */
    public static final int CATEGORY_PLATFORM = 1;
    public static final int CATEGORY_ACTIVITY = 2;
    public static final int CATEGORY_POLICY = 3;
    public static final int CATEGORY_MAINTAIN = 4;
}
