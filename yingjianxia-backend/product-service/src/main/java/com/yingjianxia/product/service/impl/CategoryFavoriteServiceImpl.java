package com.yingjianxia.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.common.core.result.ResultCode;
import com.yingjianxia.product.dto.ProductDetailResp;
import com.yingjianxia.product.dto.ProductQueryReq;
import com.yingjianxia.product.entity.Category;
import com.yingjianxia.product.entity.Product;
import com.yingjianxia.product.entity.ProductFavorite;
import com.yingjianxia.product.enums.ProductErrorCode;
import com.yingjianxia.product.mapper.CategoryMapper;
import com.yingjianxia.product.mapper.ProductFavoriteMapper;
import com.yingjianxia.product.service.CategoryFavoriteService;
import com.yingjianxia.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类 + 收藏实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryFavoriteServiceImpl implements CategoryFavoriteService {

    private final CategoryMapper categoryMapper;
    private final ProductFavoriteMapper favoriteMapper;
    private final ProductService productService;

    @Override
    public List<Category> listAllEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, Category.STATUS_ENABLE)
                .orderByAsc(Category::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFavorite(Long userId, Long productId) {
        // 检查商品存在且在售
        var detail = productService.getDetail(productId, userId);
        if (detail == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        try {
            ProductFavorite f = new ProductFavorite();
            f.setUserId(userId);
            f.setProductId(productId);
            favoriteMapper.insert(f);
        } catch (DuplicateKeyException dup) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long productId) {
        ProductFavorite f = favoriteMapper.selectOne(new LambdaQueryWrapper<ProductFavorite>()
                .eq(ProductFavorite::getUserId, userId)
                .eq(ProductFavorite::getProductId, productId));
        if (f == null) {
            throw new BusinessException(ProductErrorCode.FAVORITE_NOT_EXISTS);
        }
        favoriteMapper.deleteById(f.getId());
    }

    @Override
    public PageResult<ProductDetailResp> listMyFavorites(Long userId, int page, int size) {
        // 1. 分页查我收藏的 productId 列表
        Page<ProductFavorite> favPage = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProductFavorite>()
                        .eq(ProductFavorite::getUserId, userId)
                        .orderByDesc(ProductFavorite::getCreatedAt));
        List<Long> productIds = favPage.getRecords().stream()
                .map(ProductFavorite::getProductId).toList();
        if (productIds.isEmpty()) {
            return PageResult.of((long) page, (long) size, favPage.getTotal(), List.of());
        }
        // 2. 复用 query 查（状态=在售；注意：已下架/删除的收藏不会出现在列表中，但仍保留收藏）
        ProductQueryReq req = new ProductQueryReq();
        req.setPageNum((long) page);
        req.setPageSize((long) size);
        // 走独立方式 — 根据 productIds 查详情
        List<ProductDetailResp> list = productIds.stream()
                .map(pid -> {
                    try {
                        return productService.getDetail(pid, userId);
                    } catch (BusinessException e) {
                        return null; // 已下架/删除的跳过显示
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return PageResult.of((long) page, (long) size, favPage.getTotal(), list);
    }

    @Override
    public boolean isFavored(Long userId, Long productId) {
        Long n = favoriteMapper.selectCount(new LambdaQueryWrapper<ProductFavorite>()
                .eq(ProductFavorite::getUserId, userId)
                .eq(ProductFavorite::getProductId, productId));
        return n != null && n > 0;
    }
}
