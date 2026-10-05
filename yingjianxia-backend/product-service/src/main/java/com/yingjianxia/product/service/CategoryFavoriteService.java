package com.yingjianxia.product.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.product.dto.ProductDetailResp;
import com.yingjianxia.product.dto.ProductQueryReq;
import com.yingjianxia.product.entity.Category;

import java.util.List;

/**
 * 分类 & 收藏服务接口
 */
public interface CategoryFavoriteService {

    /* ==================== 分类 ==================== */

    /**
     * 全部启用的分类（前端导航）
     */
    List<Category> listAllEnabled();

    /* ==================== 收藏 ==================== */

    /**
     * 收藏商品
     *
     * @param userId    用户
     * @param productId 商品
     * @return true=成功收藏，false=已收藏
     */
    boolean addFavorite(Long userId, Long productId);

    /**
     * 取消收藏
     */
    void removeFavorite(Long userId, Long productId);

    /**
     * 我收藏的商品列表（分页）
     */
    PageResult<ProductDetailResp> listMyFavorites(Long userId, int page, int size);

    /**
     * 判断某用户是否收藏了某商品（详情页"已收藏"标记）
     */
    boolean isFavored(Long userId, Long productId);
}
