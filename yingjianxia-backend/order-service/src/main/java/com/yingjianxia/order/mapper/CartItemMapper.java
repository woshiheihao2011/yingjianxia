package com.yingjianxia.order.mapper;

import com.yingjianxia.common.mybatis.base.BaseRepository;
import com.yingjianxia.order.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartItemMapper extends BaseRepository<CartItem> {

    /**
     * 恢复已逻辑删除的购物车项（绕过 @TableLogic 自动过滤）
     * 用于解决：购物车清空（软删除）后，再次添加同款商品时唯一约束冲突的问题
     */
    @Update("UPDATE cart_items SET deleted = 0, quantity = #{quantity}, is_selected = #{isSelected}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND product_id = #{productId} AND deleted = 1")
    int restoreDeletedCartItem(@Param("userId") Long userId,
                               @Param("productId") Long productId,
                               @Param("quantity") Integer quantity,
                               @Param("isSelected") Integer isSelected);
}
