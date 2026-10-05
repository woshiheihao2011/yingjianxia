package com.yingjianxia.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户关注 — user_follows 表
 * <p>
 * 唯一约束：follower_id + following_id（防重复关注）
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_follows")
public class UserFollow extends BaseEntity {

    /** 关注者 */
    private Long followerId;

    /** 被关注者 */
    private Long followingId;

    /* ========== updatedAt/deleted/version 由 BaseEntity 继承，表已有对应列 ========== */
}
