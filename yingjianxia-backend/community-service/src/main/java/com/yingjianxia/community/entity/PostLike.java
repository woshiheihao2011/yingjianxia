package com.yingjianxia.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子点赞 — post_likes 表
 * <p>
 * 唯一约束：post_id + user_id（防重复点赞）
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post_likes")
public class PostLike extends BaseEntity {

    /** 关联帖子 */
    private Long postId;

    /** 点赞用户 */
    private Long userId;

    /* ========== updatedAt/deleted/version 由 BaseEntity 继承，表已有对应列 ========== */
}
