package com.yingjianxia.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子评论 — post_comments 表
 * <p>
 * 支持 parent_id 回复，status：1正常 2删除
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post_comments")
public class PostComment extends BaseEntity {

    /** 关联帖子 */
    private Long postId;

    /** 评论用户 */
    private Long userId;

    /** 父评论ID（用于回复） */
    private Long parentId;

    /** 回复目标用户ID */
    private Long replyToId;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 1正常 2删除 */
    private Integer status;

    /* ========== updatedAt/deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 状态常量 ========== */
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 2;
}
