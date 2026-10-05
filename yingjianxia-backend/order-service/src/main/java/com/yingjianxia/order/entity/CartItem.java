package com.yingjianxia.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车表 — cart_items
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart_items")
public class CartItem extends BaseEntity {

    /** 买家ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 数量 */
    private Integer quantity;

    /** 是否勾选 */
    private Integer isSelected;

    /* ========== deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    public static final int SELECTED_YES = 1;
    public static final int SELECTED_NO = 0;
}
