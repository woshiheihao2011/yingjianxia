package com.yingjianxia.marketing.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 营销积分域错误码（7001~7025）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum MarketingErrorCode implements IErrorCode {

    /* ========== 优惠券相关 7001~7015 ========== */
    COUPON_NOT_FOUND(7001, "优惠券不存在"),
    COUPON_ALREADY_CLAIMED(7002, "您已领取过该优惠券"),
    COUPON_STOCK_EMPTY(7003, "优惠券已被抢光"),
    COUPON_USED(7004, "优惠券已使用"),
    COUPON_EXPIRED(7005, "优惠券已过期"),
    COUPON_NOT_ACTIVE(7006, "优惠券活动未进行中"),
    COUPON_NOT_CLAIMED(7007, "您未领取该优惠券，无法使用"),
    COUPON_MIN_SPEND_NOT_MET(7008, "订单金额未达到优惠券使用门槛"),
    COUPON_PER_USER_LIMIT(7009, "已达每人限领数量"),
    COUPON_SCOPE_NOT_MATCH(7010, "优惠券不适用于当前商品"),
    COUPON_RECORD_NOT_FOUND(7011, "领券记录不存在"),
    COUPON_REFUND_FAIL(7012, "优惠券退还失败（可能已被使用）"),
    COUPON_TIME_INVALID(7013, "优惠券有效期设置不合法"),
    COUPON_TYPE_PARAM_MISSING(7014, "优惠券类型参数缺失（满减需面额，折扣需折扣率）"),
    COUPON_NO_PERMISSION(7015, "无权操作该优惠券"),

    /* ========== 积分相关 7016~7020 ========== */
    POINTS_ACCOUNT_NOT_FOUND(7016, "积分账户不存在"),
    POINTS_INSUFFICIENT(7017, "积分不足"),
    POINTS_SIGNIN_DUPLICATE(7018, "今日已签到，请勿重复签到"),
    POINTS_DEDUCT_AMOUNT_INVALID(7019, "积分扣减数量必须大于0"),
    POINTS_REWARD_AMOUNT_INVALID(7020, "积分奖励数量必须大于0"),

    /* ========== 营销活动相关 7021~7025 ========== */
    PROMOTION_NOT_FOUND(7021, "营销活动不存在"),
    PROMOTION_TIME_INVALID(7022, "活动时间设置不合法"),
    PROMOTION_NOT_ACTIVE(7023, "活动未在进行中"),
    PROMOTION_STOCK_EMPTY(7024, "活动库存已售罄"),
    PROMOTION_NO_PERMISSION(7025, "无权操作该营销活动");

    private final Integer code;
    private final String message;
}
