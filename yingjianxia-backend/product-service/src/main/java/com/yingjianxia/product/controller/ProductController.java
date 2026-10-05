package com.yingjianxia.product.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.product.dto.BatchImportResult;
import com.yingjianxia.product.dto.BatchPriceUpdateReq;
import com.yingjianxia.product.dto.BatchStockUpdateReq;
import com.yingjianxia.product.dto.ProductDetailResp;
import com.yingjianxia.product.dto.ProductQueryReq;
import com.yingjianxia.product.dto.ProductSaveReq;
import com.yingjianxia.product.dto.SellerStatsResp;
import com.yingjianxia.product.entity.Category;
import com.yingjianxia.product.service.CategoryFavoriteService;
import com.yingjianxia.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品模块 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "商品模块", description = "商品发布/编辑/状态流转/详情/搜索/库存扣减")
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final ProductService productService;
    private final CategoryFavoriteService categoryFavoriteService;

    /* ======================== 卖家端 ======================== */

    @Operation(summary = "新增商品（保存为草稿或提交审核）")
    @PostMapping
    public ApiResponse<Long> createProduct(@Valid @RequestBody ProductSaveReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(productService.saveProduct(sellerId, null, req));
    }

    @Operation(summary = "编辑商品（保存为草稿或提交审核）")
    @PutMapping("/{productId}")
    public ApiResponse<Long> updateProduct(@PathVariable("productId") Long productId,
                                           @Valid @RequestBody ProductSaveReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(productService.saveProduct(sellerId, productId, req));
    }

    @Operation(summary = "提交审核（草稿/审核拒绝→审核中）")
    @PostMapping("/{productId}/submit-review")
    public ApiResponse<Void> submitForReview(@PathVariable("productId") Long productId) {
        Long sellerId = UserContext.requiredUserId();
        productService.submitForReview(sellerId, productId);
        return ApiResponse.success();
    }

    @Operation(summary = "下架商品（在售→下架）")
    @PostMapping("/{productId}/off-shelf")
    public ApiResponse<Void> offShelf(@PathVariable("productId") Long productId) {
        Long sellerId = UserContext.requiredUserId();
        productService.offShelf(sellerId, productId);
        return ApiResponse.success();
    }

    @Operation(summary = "重新上架（下架→重新提交审核）")
    @PostMapping("/{productId}/re-submit")
    public ApiResponse<Void> reSubmit(@PathVariable("productId") Long productId) {
        Long sellerId = UserContext.requiredUserId();
        productService.reSubmitForReview(sellerId, productId);
        return ApiResponse.success();
    }

    @Operation(summary = "删除商品（仅草稿/拒绝/下架允许）")
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable("productId") Long productId) {
        Long sellerId = UserContext.requiredUserId();
        productService.deleteProduct(sellerId, productId);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 我发布的商品列表（按状态筛选）")
    @GetMapping("/mine")
    public ApiResponse<PageResult<ProductDetailResp>> myProducts(@ParameterObject ProductQueryReq req) {
        Long sellerId = UserContext.requiredUserId();
        req.setSellerId(sellerId);
        return ApiResponse.success(productService.query(req));
    }

    /* ======================== 买家端（公开） ======================== */

    @Operation(summary = "商品分类列表（兼容 /product/categories 路径）")
    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.success(categoryFavoriteService.listAllEnabled());
    }

    @Operation(summary = "商品详情（浏览量+1）")
    @GetMapping("/{productId:\\d+}")
    public ApiResponse<ProductDetailResp> getDetail(@PathVariable("productId") Long productId) {
        Long viewerId = UserContext.currentUserId();
        return ApiResponse.success(productService.getDetail(productId, viewerId));
    }

    @Operation(summary = "[内部] 商品详情（不过滤状态、不增加浏览量）")
    @GetMapping("/internal/{productId:\\d+}")
    public ApiResponse<ProductDetailResp> getInternalDetail(@PathVariable("productId") Long productId) {
        return ApiResponse.success(productService.getDetailInternal(productId));
    }

    @Operation(summary = "增加浏览量")
    @PostMapping("/{productId}/view")
    public ApiResponse<Void> incrementView(@PathVariable("productId") Long productId) {
        Long viewerId = UserContext.currentUserId();
        productService.incrementViewCount(productId);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 库存管理列表（低库存预警）")
    @GetMapping("/inventory")
    public ApiResponse<PageResult<ProductDetailResp>> inventoryList(@ParameterObject ProductQueryReq req) {
        Long sellerId = UserContext.requiredUserId();
        req.setSellerId(sellerId);
        return ApiResponse.success(productService.query(req));
    }

    @Operation(summary = "商品列表/搜索（ES→MySQL 降级）")
    @GetMapping("/search")
    public ApiResponse<PageResult<ProductDetailResp>> search(@ParameterObject ProductQueryReq req) {
        return ApiResponse.success(productService.query(req));
    }

    @Operation(summary = "热搜词列表")
    @GetMapping("/hot-keywords")
    public ApiResponse<List<String>> hotKeywords() {
        // 返回静态热搜词列表（后续可从 Redis ZSet 获取）
        List<String> keywords = List.of("RTX 4090", "iPhone 15 Pro", "PS5", "Switch", "RTX 4070", "MacBook Pro", "iPad Air", "显示器");
        return ApiResponse.success(keywords);
    }

    @Operation(summary = "[卖家端] 批量下架")
    @PostMapping("/batch-off-shelf")
    public ApiResponse<Void> batchOffShelf(@RequestBody BatchProductReq req) {
        Long sellerId = UserContext.requiredUserId();
        for (Long pid : req.getProductIds()) {
            try {
                productService.offShelf(sellerId, pid);
            } catch (Exception e) {
                // 单个失败不影响其他
            }
        }
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 批量删除")
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody BatchProductReq req) {
        Long sellerId = UserContext.requiredUserId();
        for (Long pid : req.getProductIds()) {
            try {
                productService.deleteProduct(sellerId, pid);
            } catch (Exception e) {
                // 单个失败不影响其他
            }
        }
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 批量上架（重新提交审核）")
    @PostMapping("/batch-relist")
    public ApiResponse<Void> batchRelist(@RequestBody BatchProductReq req) {
        Long sellerId = UserContext.requiredUserId();
        for (Long pid : req.getProductIds()) {
            try {
                productService.reSubmitForReview(sellerId, pid);
            } catch (Exception e) {
                // 单个失败不影响其他
            }
        }
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 批量改价")
    @PostMapping("/batch-update-price")
    public ApiResponse<Void> batchUpdatePrice(@Valid @RequestBody BatchPriceUpdateReq req) {
        Long sellerId = UserContext.requiredUserId();
        productService.batchUpdatePrice(sellerId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 批量改库存")
    @PostMapping("/batch-update-stock")
    public ApiResponse<Void> batchUpdateStock(@Valid @RequestBody BatchStockUpdateReq req) {
        Long sellerId = UserContext.requiredUserId();
        productService.batchUpdateStock(sellerId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家端] 我的商品统计")
    @GetMapping("/mine/stats")
    public ApiResponse<SellerStatsResp> myStats() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(productService.getSellerStats(sellerId));
    }

    @Operation(summary = "[卖家端] 批量导入商品（Excel/CSV）")
    @PostMapping(value = "/batch-import", consumes = "multipart/form-data")
    public ApiResponse<BatchImportResult> batchImport(@RequestParam("file") MultipartFile file) {
        Long sellerId = UserContext.requiredUserId();
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail(2000, "请选择要导入的文件");
        }
        return ApiResponse.success(productService.batchImport(sellerId, file));
    }

    /** 批量操作请求体 */
    @lombok.Data
    public static class BatchProductReq {
        private List<Long> productIds;
    }

    /* ======================== 内部（订单 TCC / 审核服务） ======================== */

    @Operation(summary = "[内部] 审核通过", hidden = true)
    @PostMapping("/internal/{productId}/audit-pass")
    public ApiResponse<Void> auditPass(@PathVariable Long productId,
                                       @RequestHeader("X-Auditor-Id") Long auditorId,
                                       @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.success();
        productService.auditPass(productId, auditorId);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] 审核拒绝", hidden = true)
    @PostMapping("/internal/{productId}/audit-reject")
    public ApiResponse<Void> auditReject(@PathVariable Long productId,
                                         @RequestHeader("X-Auditor-Id") Long auditorId,
                                         @RequestParam String reason,
                                         @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.success();
        productService.auditReject(productId, auditorId, reason);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] Try-预扣库存（订单TCC）", hidden = true)
    @PostMapping("/internal/stock/{productId}/try-deduct")
    public ApiResponse<Boolean> tryStock(@PathVariable Long productId,
                                         @RequestParam int qty,
                                         @RequestParam String idempotentKey,
                                         @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(productService.tryDeductStock(productId, qty, idempotentKey));
    }

    @Operation(summary = "[内部] Confirm-确认扣减（订单TCC）", hidden = true)
    @PostMapping("/internal/stock/{productId}/confirm-deduct")
    public ApiResponse<Boolean> confirmStock(@PathVariable Long productId,
                                             @RequestParam int qty,
                                             @RequestParam String idempotentKey,
                                             @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(productService.confirmDeductStock(productId, qty, idempotentKey));
    }

    @Operation(summary = "[内部] Cancel-释放库存（订单TCC）", hidden = true)
    @PostMapping("/internal/stock/{productId}/cancel-deduct")
    public ApiResponse<Boolean> cancelStock(@PathVariable Long productId,
                                            @RequestParam int qty,
                                            @RequestParam String idempotentKey,
                                            @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(productService.cancelDeductStock(productId, qty, idempotentKey));
    }

    @Operation(summary = "[内部] 生成商品快照（下单时）", hidden = true)
    @PostMapping("/internal/{productId}/snapshot")
    public ApiResponse<Long> createSnapshot(@PathVariable Long productId,
                                            @RequestParam Long orderId,
                                            @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(productService.createSnapshot(productId, orderId));
    }

    @Operation(summary = "[内部] 手动触发浏览量回写", hidden = true)
    @PostMapping("/internal/sync-view-count")
    public ApiResponse<Void> syncViewCount(@RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.success();
        productService.syncViewCountFromRedis();
        return ApiResponse.success();
    }
}
