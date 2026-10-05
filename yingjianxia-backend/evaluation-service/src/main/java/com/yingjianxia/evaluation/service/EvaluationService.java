package com.yingjianxia.evaluation.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.evaluation.dto.ReviewCreateReq;
import com.yingjianxia.evaluation.dto.ReviewQueryReq;
import com.yingjianxia.evaluation.dto.ReviewReplyReq;
import com.yingjianxia.evaluation.entity.Review;
import com.yingjianxia.evaluation.entity.ReviewTag;

import java.util.List;
import java.util.Map;

/**
 * 评价服务接口
 *
 * @author 硬件侠后端团队
 */
public interface EvaluationService {

    /** 发表评价（校验订单已完成+未评价过 → 插入 → 更新订单评价状态 → 给卖家加信用分事件） */
    Long createReview(ReviewCreateReq req, Long reviewerId);/**
     * 卖家回复评价
     */
    void replyReview(ReviewReplyReq req, Long sellerId);

    /**
     * 买家追评
     */
    void followUpReview(Long reviewId, Long buyerId, String content, String images);    /** 评价列表（按商品查/按卖家查，分页+评分筛选） */
    PageResult<Review> listReviews(ReviewQueryReq req);

    /** 我的评价列表（买家） */
    PageResult<Review> myReviews(Long reviewerId, long pageNum, long pageSize);

    /** 商品评价列表（公开） */
    PageResult<Review> productReviews(Long productId, Integer rating, long pageNum, long pageSize);

    /** 店铺评价列表（按卖家ID查，公开） */
    PageResult<Review> sellerReviews(Long sellerId, Integer rating, long pageNum, long pageSize);

    /** 店铺评价统计（按卖家ID查，公开） */
    Map<String, Object> sellerReviewStats(Long sellerId);

    /* ========== 评价标签管理 ========== */

    /** 标签列表（按分类） */
    List<ReviewTag> listTags(Integer category);

    /** 新增/更新标签 */
    Long saveTag(ReviewTag tag);
}
