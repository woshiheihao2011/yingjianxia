package com.yingjianxia.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品收藏实体 — product_favorites 表
 * <p>
 * 唯一约束：uk_user_product (user_id + product_id)
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_favorites")
public class ProductFavorite extends BaseEntity {

    /** 收藏用户 */
    private Long userId;

    /** 收藏商品 */
    private Long productId;

    /* ========== deleted/version/updatedAt 由 BaseEntity 继承，表已有对应列 ========== */
}
