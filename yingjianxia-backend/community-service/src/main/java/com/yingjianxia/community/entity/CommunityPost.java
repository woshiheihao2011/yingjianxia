package com.yingjianxia.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子 — community_posts 表
 * <p>
 * 分类：1装机指南 2避坑攻略 3硬件评测 4二手验机 5问答互助 6晒单
 * 状态：1正常 2审核中 3下架 4违规
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_posts")
public class CommunityPost extends BaseEntity {

    /** 作者用户ID */
    private Long authorId;

    /** 分类：1装机指南 2避坑攻略 3硬件评测 4二手验机 5问答互助 6晒单 */
    private Integer category;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 正文 */
    private String content;

    /** 封面图 */
    private String coverImage;

    /** 图片列表（JSON） */
    private String images;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 浏览数 */
    private Integer viewCount;

    /** 分享数 */
    private Integer shareCount;

    /** 精选攻略 */
    private Integer isFeatured;

    /** 热门标记 */
    private Integer isHot;

    /** 1正常 2审核中 3下架 4违规 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /* ========== deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 非持久化：图片列表（JSON解析后） ========== */
    @TableField(exist = false)
    private List<String> imageList;

    /* ========== 状态常量 ========== */
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_REVIEWING = 2;
    public static final int STATUS_OFFLINE = 3;
    public static final int STATUS_VIOLATION = 4;

    /* ========== 分类常量 ========== */
    public static final int CATEGORY_GUIDE = 1;
    public static final int CATEGORY_PITFALL = 2;
    public static final int CATEGORY_REVIEW = 3;
    public static final int CATEGORY_INSPECTION = 4;
    public static final int CATEGORY_QA = 5;
    public static final int CATEGORY_SHOW = 6;
}
