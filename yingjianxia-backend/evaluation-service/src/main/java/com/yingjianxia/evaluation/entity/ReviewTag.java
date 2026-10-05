package com.yingjianxia.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价标签 — review_tags 表（评价域自建表）
 * <p>
 * category：1好评 2中评 3差评
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_tags")
public class ReviewTag extends BaseEntity {

    /** 标签文本 */
    private String tagText;

    /** 1好评 2中评 3差评 */
    private Integer category;

    /** 使用次数 */
    private Integer useCount;

    /* ========== 本表不存在：updatedAt/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 分类常量 ========== */
    public static final int CATEGORY_POSITIVE = 1;
    public static final int CATEGORY_NEUTRAL = 2;
    public static final int CATEGORY_NEGATIVE = 3;
}
