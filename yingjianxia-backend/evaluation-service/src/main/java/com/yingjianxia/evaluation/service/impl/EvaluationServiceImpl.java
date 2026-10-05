package com.yingjianxia.evaluation.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.evaluation.dto.ReviewCreateReq;
import com.yingjianxia.evaluation.dto.ReviewQueryReq;
import com.yingjianxia.evaluation.dto.ReviewReplyReq;
import com.yingjianxia.evaluation.entity.Review;
import com.yingjianxia.evaluation.entity.ReviewTag;
import com.yingjianxia.evaluation.enums.EvaluationErrorCode;
import com.yingjianxia.evaluation.feign.OrderFeignClient;
import com.yingjianxia.evaluation.mapper.ReviewMapper;
import com.yingjianxia.evaluation.mapper.ReviewTagMapper;
import com.yingjianxia.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 评价服务实现 — 订单评价/卖家评分/卖家回复/评价标签
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final ReviewMapper reviewMapper;
    private final ReviewTagMapper tagMapper;
    private final ObjectMapper objectMapper;
    private final OrderFeignClient orderFeignClient;

    /* ======================== 发表评价 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(ReviewCreateReq req, Long reviewerId) {
        // 1. 评分合法性
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new BusinessException(EvaluationErrorCode.RATING_INVALID);
        }
        // 2. 校验订单已完成（跨服务调用 order-service 占位）
        if (!checkOrderCompleted(req.getOrderId(), reviewerId)) {
            throw new BusinessException(EvaluationErrorCode.ORDER_NOT_COMPLETED);
        }
        // 3. 校验未评价过（唯一评价）
        Long cnt = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, req.getOrderId())
                .eq(Review::getReviewerId, reviewerId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(EvaluationErrorCode.REVIEW_DUPLICATE);
        }
        // 4. 取订单关联信息（商品/卖家，跨服务占位）
        OrderInfo orderInfo = loadOrderInfo(req.getOrderId());

        // 5. 插入评价
        Review review = new Review();
        review.setOrderId(req.getOrderId());
        review.setProductId(orderInfo.productId());
        review.setSellerId(orderInfo.sellerId());
        review.setReviewerId(reviewerId);
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setImages(toJson(req.getImages()));
        review.setIsAnonymous(Boolean.TRUE.equals(req.getIsAnonymous()) ? 1 : 0);
        review.setStatus(Review.STATUS_NORMAL);
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewMapper.insert(review);

        // 6. 更新订单评价状态（跨服务调用 order-service 占位）
        markOrderReviewed(req.getOrderId(), review.getId());

        // 7. 给卖家加信用分事件（Outbox 模式发 MQ 占位）
        // TODO: OutboxPattern: send "review.created" event → user-service 加卖家信用分
        log.info("【发表评价】reviewerId={}, orderId={}, rating={}, sellerId={}",
                reviewerId, req.getOrderId(), req.getRating(), orderInfo.sellerId());
        return review.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(ReviewReplyReq req, Long sellerId) {
        Review review = requireReview(req.getReviewId());
        if (review.getSellerId() == null || !review.getSellerId().equals(sellerId)) {
            throw new BusinessException(EvaluationErrorCode.SELLER_REPLY_FORBIDDEN);
        }
        if (review.getReply() != null && !review.getReply().isBlank()) {
            throw new BusinessException(EvaluationErrorCode.REVIEW_ALREADY_REPLIED);
        }
        reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                .eq(Review::getId, req.getReviewId())
                .set(Review::getReply, req.getReplyContent())
                .set(Review::getReplyAt, LocalDateTime.now())
                .set(Review::getUpdatedAt, LocalDateTime.now()));
        log.info("【卖家回复评价】reviewId={}, sellerId={}", req.getReviewId(), sellerId);
    }

    @Override
    public void followUpReview(Long reviewId, Long buyerId, String content, String images) {
        Review review = requireReview(reviewId);
        if (!review.getReviewerId().equals(buyerId)) {
            throw new BusinessException(EvaluationErrorCode.REVIEW_NOT_FOUND);
        }
        // 简化：将追评内容追加到 content（实际可拆分为独立追评表）
        String appended = review.getContent() + "\n[追评] " + content;
        reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                .eq(Review::getId, reviewId)
                .set(Review::getContent, appended)
                .set(Review::getUpdatedAt, java.time.LocalDateTime.now()));
        log.info("【买家追评】reviewId={}, buyerId={}", reviewId, buyerId);
    }

    /* ======================== 评价列表 ======================== */

    @Override
    public PageResult<Review> listReviews(ReviewQueryReq req) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<Review>()
                .eq(Review::getStatus, Review.STATUS_NORMAL)
                .eq(req.getProductId() != null, Review::getProductId, req.getProductId())
                .eq(req.getSellerId() != null, Review::getSellerId, req.getSellerId())
                .eq(req.getRating() != null, Review::getRating, req.getRating())
                .orderByDesc(Review::getCreatedAt);
        IPage<Review> page = reviewMapper.selectPage(new Page<>(req.getPageNum(), req.getPageSize()), qw);
        page.getRecords().forEach(this::fillImages);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<Review> myReviews(Long reviewerId, long pageNum, long pageSize) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<Review>()
                .eq(Review::getReviewerId, reviewerId)
                .orderByDesc(Review::getCreatedAt);
        IPage<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        page.getRecords().forEach(this::fillImages);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<Review> productReviews(Long productId, Integer rating, long pageNum, long pageSize) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .eq(Review::getStatus, Review.STATUS_NORMAL)
                .eq(rating != null, Review::getRating, rating)
                .orderByDesc(Review::getCreatedAt);
        IPage<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        page.getRecords().forEach(this::fillImages);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<Review> sellerReviews(Long sellerId, Integer rating, long pageNum, long pageSize) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<Review>()
                .eq(Review::getSellerId, sellerId)
                .eq(Review::getStatus, Review.STATUS_NORMAL)
                .eq(rating != null, Review::getRating, rating)
                .orderByDesc(Review::getCreatedAt);
        IPage<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        page.getRecords().forEach(this::fillImages);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    public Map<String, Object> sellerReviewStats(Long sellerId) {
        // 查询该卖家所有正常状态的评价
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getSellerId, sellerId)
                .eq(Review::getStatus, Review.STATUS_NORMAL));

        Map<String, Object> stats = new java.util.HashMap<>();
        int totalCount = reviews.size();
        stats.put("sellerId", sellerId);
        stats.put("totalCount", totalCount);

        if (totalCount == 0) {
            stats.put("averageRating", 0.0);
            stats.put("positiveRate", 0.0);
            Map<String, Integer> empty = new java.util.HashMap<>();
            for (int i = 1; i <= 5; i++) empty.put(String.valueOf(i), 0);
            stats.put("ratingDistribution", empty);
            return stats;
        }

        // 平均评分
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        stats.put("averageRating", Math.round(avg * 10.0) / 10.0);

        // 好评率（rating >= 4 的占比）
        long positive = reviews.stream().filter(r -> r.getRating() != null && r.getRating() >= 4).count();
        stats.put("positiveRate", Math.round((double) positive / totalCount * 1000.0) / 10.0);

        // 评分分布
        Map<String, Integer> distribution = new java.util.HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int level = i;
            distribution.put(String.valueOf(i),
                    (int) reviews.stream().filter(r -> r.getRating() != null && r.getRating() == level).count());
        }
        stats.put("ratingDistribution", distribution);

        return stats;
    }

    /* ======================== 评价标签管理 ======================== */

    @Override
    public List<ReviewTag> listTags(Integer category) {
        try {
            return tagMapper.selectList(new LambdaQueryWrapper<ReviewTag>()
                    .eq(category != null, ReviewTag::getCategory, category)
                    .orderByDesc(ReviewTag::getUseCount));
        } catch (Exception e) {
            log.warn("【评价标签查询失败】返回空列表, category={}, err={}", category, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTag(ReviewTag tag) {
        LocalDateTime now = LocalDateTime.now();
        if (tag.getId() == null) {
            if (tag.getUseCount() == null) tag.setUseCount(0);
            tag.setCreatedAt(now);
            tag.setUpdatedAt(now);
            tagMapper.insert(tag);
        } else {
            tag.setUpdatedAt(now);
            tagMapper.updateById(tag);
        }
        return tag.getId();
    }

    /* ======================== 内部工具 ======================== */

    private Review requireReview(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(EvaluationErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    /**
     * 校验订单已完成 — 通过 Feign 调用 order-service
     */
    private boolean checkOrderCompleted(Long orderId, Long reviewerId) {
        if (orderId == null) return false;
        try {
            ApiResponse<Map<String, Object>> resp = orderFeignClient.getOrderDetail(orderId);
            if (resp == null || resp.getData() == null) return false;
            Map<String, Object> data = resp.getData();
            // 校验订单状态 = 已完成(4) 且买家匹配
            Object statusObj = data.get("status");
            Object buyerIdObj = data.get("buyerId");
            int status = statusObj != null ? Integer.parseInt(statusObj.toString()) : -1;
            Long buyerId = buyerIdObj != null ? Long.parseLong(buyerIdObj.toString()) : null;
            return status == 4 && reviewerId.equals(buyerId);
        } catch (Exception e) {
            log.warn("【评价】Feign 校验订单状态失败, orderId={}, err={}", orderId, e.getMessage());
            return false;
        }
    }

    /**
     * 加载订单关联信息（商品/卖家） — 通过 Feign 调用 order-service
     */
    private OrderInfo loadOrderInfo(Long orderId) {
        try {
            ApiResponse<Map<String, Object>> resp = orderFeignClient.getOrderDetail(orderId);
            if (resp != null && resp.getData() != null) {
                Map<String, Object> data = resp.getData();
                Object productIdObj = data.get("productId");
                Object sellerIdObj = data.get("sellerId");
                Long productId = productIdObj != null ? Long.parseLong(productIdObj.toString()) : 0L;
                Long sellerId = sellerIdObj != null ? Long.parseLong(sellerIdObj.toString()) : 0L;
                return new OrderInfo(productId, sellerId);
            }
        } catch (Exception e) {
            log.warn("【评价】Feign 获取订单信息失败, orderId={}, err={}", orderId, e.getMessage());
        }
        throw new BusinessException(13008, "订单不存在或获取失败");
    }

    /**
     * 更新订单评价状态 — 跨服务调用 order-service 占位
     */
    private void markOrderReviewed(Long orderId, Long reviewId) {
        // TODO: Feign 调用 order-service 标记订单已评价
        log.debug("【更新订单评价状态占位】orderId={}, reviewId={}", orderId, reviewId);
    }

    private String toJson(List<String> images) {
        if (CollUtil.isEmpty(images)) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void fillImages(Review review) {
        if (review.getImages() != null && !review.getImages().isBlank()) {
            try {
                review.setImageList(objectMapper.readValue(review.getImages(), new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                review.setImageList(Collections.emptyList());
            }
        }
    }

    /** 订单关联信息载体 */
    private record OrderInfo(Long productId, Long sellerId) {
    }
}
