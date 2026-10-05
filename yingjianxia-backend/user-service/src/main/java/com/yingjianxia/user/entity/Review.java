package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价实体 — reviews 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reviews")
public class Review extends BaseEntity {

    /**
     * 关联订单
     */
    private Long orderId;

    /**
     * 评价商品
     */
    private Long productId;

    /**
     * 评价人（买家）
     */
    private Long reviewerId;

    /**
     * 被评价卖家
     */
    private Long sellerId;

    /**
     * 1-5 星
     */
    private Integer rating;

    /**
     * 评价标签 JSON 数组
     */
    private String tags;

    /**
     * 评价文字
     */
    private String content;

    /**
     * 评价图片 URL JSON 数组
     */
    private String images;

    /**
     * 匿名评价
     */
    private Boolean isAnonymous;

    /**
     * 卖家回复内容
     */
    private String replyContent;

    /**
     * 卖家回复时间
     */
    private LocalDateTime replyAt;

    /**
     * 买家追评内容
     */
    private String appendContent;

    /**
     * 追评时间
     */
    private LocalDateTime appendAt;

    /* ========== 覆盖 BaseEntity 中本表不存在的字段 ========== */

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 非持久化便捷字段（JSON解析后） ========== */

    @TableField(exist = false)
    private List<String> tagList;

    @TableField(exist = false)
    private List<String> imageList;
}
