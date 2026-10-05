package com.yingjianxia.evaluation.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.evaluation.dto.ReviewCreateReq;
import com.yingjianxia.evaluation.dto.ReviewQueryReq;
import com.yingjianxia.evaluation.dto.ReviewReplyReq;
import com.yingjianxia.evaluation.entity.Review;
import com.yingjianxia.evaluation.entity.ReviewTag;
import com.yingjianxia.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价服务 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "评价服务", description = "买家：发表评价/我的评价；卖家：回复评价/评价列表；公开：商品评价列表")
@RestController
@RequestMapping("/api/v1/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService service;

    /* ========== 买家端 ========== */

    @Operation(summary = "[买家] 发表评价")
    @PostMapping("/reviews")
    public ApiResponse<Long> createReview(@Valid @RequestBody ReviewCreateReq req) {
        return ApiResponse.success(service.createReview(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[买家] 我的评价列表")
    @GetMapping("/reviews/mine")
    public ApiResponse<PageResult<Review>> myReviews(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.myReviews(UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "[买家] 追评")
    @PostMapping("/reviews/{reviewId}/follow-up")
    public ApiResponse<Void> followUp(@PathVariable Long reviewId,
                                      @RequestBody java.util.Map<String, Object> body) {
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));
        String images = body.get("images") == null ? null : String.valueOf(body.get("images"));
        service.followUpReview(reviewId, UserContext.requiredUserId(), content, images);
        return ApiResponse.success();
    }

    /* ========== 卖家端 ========== */

    @Operation(summary = "[卖家] 回复评价")
    @PostMapping("/reviews/reply")
    public ApiResponse<Void> replyReview(@Valid @RequestBody ReviewReplyReq req) {
        service.replyReview(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 评价列表（按卖家/评分查询）")
    @GetMapping("/reviews")
    public ApiResponse<PageResult<Review>> listReviews(ReviewQueryReq req) {
        return ApiResponse.success(service.listReviews(req));
    }

    /* ========== 公开 ========== */

    @Operation(summary = "[公开] 商品评价列表")
    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<PageResult<Review>> productReviews(
            @PathVariable Long productId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.productReviews(productId, rating, pageNum, pageSize));
    }

    @Operation(summary = "[公开] 店铺评价列表（按卖家ID查）")
    @GetMapping("/sellers/{sellerId}/reviews")
    public ApiResponse<PageResult<Review>> sellerReviews(
            @PathVariable Long sellerId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.sellerReviews(sellerId, rating, pageNum, pageSize));
    }

    @Operation(summary = "[公开] 店铺评价统计（好评率/评分分布/评价总数）")
    @GetMapping("/sellers/{sellerId}/stats")
    public ApiResponse<java.util.Map<String, Object>> sellerReviewStats(@PathVariable Long sellerId) {
        return ApiResponse.success(service.sellerReviewStats(sellerId));
    }

    /* ========== 评价标签 ========== */

    @Operation(summary = "评价标签列表（按分类）")
    @GetMapping("/tags")
    public ApiResponse<List<ReviewTag>> listTags(@RequestParam(value = "category", required = false) Integer category) {
        return ApiResponse.success(service.listTags(category));
    }

    @Operation(summary = "[管理端] 新增/更新评价标签")
    @PostMapping("/tags")
    public ApiResponse<Long> saveTag(@RequestBody ReviewTag tag) {
        return ApiResponse.success(service.saveTag(tag));
    }
}
