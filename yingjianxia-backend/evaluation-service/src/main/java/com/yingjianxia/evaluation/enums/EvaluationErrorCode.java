package com.yingjianxia.evaluation.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价服务错误码（13001~13010）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum EvaluationErrorCode implements IErrorCode {
    REVIEW_NOT_FOUND(13001, "评价不存在"),
    REVIEW_DUPLICATE(13002, "该订单已评价，不能重复评价"),
    ORDER_NOT_COMPLETED(13003, "订单未完成，不可评价"),
    REVIEW_NO_PERMISSION(13004, "无权操作该评价"),
    REVIEW_ALREADY_REPLIED(13005, "卖家已回复，不能重复回复"),
    RATING_INVALID(13006, "评分不合法（1~5星）"),
    NOT_ORDER_BUYER(13007, "只有订单买家可评价"),
    SELLER_REPLY_FORBIDDEN(13008, "只有该评价对应的卖家可回复"),
    REVIEW_HIDDEN(13009, "该评价已隐藏"),
    TAG_NOT_FOUND(13010, "评价标签不存在");

    private final Integer code;
    private final String message;
}
