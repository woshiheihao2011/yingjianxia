package com.yingjianxia.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品图片实体 — product_images 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_images")
public class ProductImage extends BaseEntity {

    /** 关联商品 */
    private Long productId;

    /** 图片 URL（建议 WebP 格式，CDN 加速） */
    private String imageUrl;

    /** 排序（第一张为主图） */
    private Integer sortOrder;

    /** 是否主图 */
    private Boolean isCover;

    /* ========== deleted/version/updatedAt 由 BaseEntity 继承，表已有对应列 ========== */
}
