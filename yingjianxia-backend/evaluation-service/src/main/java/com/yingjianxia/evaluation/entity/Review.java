package com.yingjianxia.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价 — reviews 表（评价域自建表）
 * <p>
 * 评分 rating：1~5 星
 * 状态 status：1正常 2隐藏 3违规
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reviews")
public class Review extends BaseEntity {

    /** 关联订单 */
    private Long orderId;

    /** 评价商品 */
    private Long productId;

    /** 被评价卖家 */
    private Long sellerId;

    /** 评价人（买家） */
    private Long reviewerId;

    /** 评分 1~5 星 */
    private Integer rating;

    /** 评价文字 */
    private String content;

    /** 评价图片 URL JSON 数组 */
    private String images;

    /** 匿名评价 */
    private Integer isAnonymous;

    /** 1正常 2隐藏 3违规 */
    private Integer status;

    /** 卖家回复内容 */
    private String reply;

    /** 卖家回复时间 */
    private LocalDateTime replyAt;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 非持久化：图片列表（JSON解析后） ========== */
    @TableField(exist = false)
    private List<String> imageList;

    /* ========== 状态常量 ========== */
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_HIDDEN = 2;
    public static final int STATUS_VIOLATION = 3;
}
