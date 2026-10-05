package com.yingjianxia.product.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.product.dto.ProductDetailResp;
import com.yingjianxia.product.entity.Category;
import com.yingjianxia.product.service.CategoryFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类 & 收藏 Controller
 */
@Tag(name = "分类与收藏", description = "商品分类、收藏夹管理")
@RestController
@RequiredArgsConstructor
public class CategoryFavoriteController {

    private final CategoryFavoriteService service;

    /* ========== 分类（公开） ========== */

    @Operation(summary = "全部分类（启用的）")
    @GetMapping("/api/v1/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.success(service.listAllEnabled());
    }

    /* ========== 收藏（买家端） ========== */

    @Operation(summary = "收藏商品")
    @PostMapping({"/api/v1/favorite/{productId}", "/api/v1/favorites"})
    public ApiResponse<Boolean> addFavorite(@PathVariable(value = "productId", required = false) Long productId,
                                            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long uid = UserContext.requiredUserId();
        Long pid = productId != null ? productId : Long.valueOf(String.valueOf(body.get("productId")));
        return ApiResponse.success(service.addFavorite(uid, pid));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping({"/api/v1/favorite/{productId}", "/api/v1/favorites/{productId}"})
    public ApiResponse<Void> removeFavorite(@PathVariable("productId") Long productId) {
        Long uid = UserContext.requiredUserId();
        service.removeFavorite(uid, productId);
        return ApiResponse.success();
    }

    @Operation(summary = "我收藏的商品列表")
    @GetMapping("/api/v1/favorites")
    public ApiResponse<PageResult<ProductDetailResp>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long uid = UserContext.requiredUserId();
        return ApiResponse.success(service.listMyFavorites(uid, page, size));
    }

    @Operation(summary = "是否收藏某商品（详情页标记用）")
    @GetMapping("/api/v1/favorite/{productId}/check")
    public ApiResponse<Boolean> checkFavorite(@PathVariable("productId") Long productId) {
        Long uid = UserContext.requiredUserId();
        return ApiResponse.success(service.isFavored(uid, productId));
    }
}
