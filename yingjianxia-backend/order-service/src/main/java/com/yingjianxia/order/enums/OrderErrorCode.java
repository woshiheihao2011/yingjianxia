package com.yingjianxia.order.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单交易域错误码（4000~4999）
 */
@Getter
@AllArgsConstructor
public enum OrderErrorCode implements IErrorCode {
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_ITEM_EMPTY(4002, "下单商品列表不能为空"),
    INSUFFICIENT_STOCK(4003, "商品库存不足"),
    INVALID_STATUS_TRANSITION(4004, "订单状态流转不合法"),
    DUPLICATE_ORDER(4005, "不能重复下单（幂等键已存在）"),
    ORDER_ALREADY_PAID(4006, "订单已支付，不能重复支付"),
    ORDER_ALREADY_CANCELLED(4007, "订单已取消"),
    ORDER_ALREADY_COMPLETED(4008, "订单已完成"),
    NOT_BUYER_OF_ORDER(4009, "您不是该订单的买家，无权操作"),
    NOT_SELLER_OF_ORDER(4010, "您不是该订单的卖家，无权操作"),
    CANCEL_NOT_ALLOWED(4011, "当前订单状态不允许取消"),
    CONFIRM_NOT_ALLOWED(4012, "当前订单状态不允许确认收货"),
    SHIP_NOT_ALLOWED(4013, "当前订单状态不允许发货"),
    ADDRESS_REQUIRED(4014, "收货地址不能为空"),
    QUANTITY_INVALID(4015, "商品数量不合法"),
    PRICE_MISMATCH(4016, "订单价格校验不一致"),
    COUPON_INVALID(4017, "优惠券不可用"),
    POINTS_INSUFFICIENT(4018, "用户积分不足"),
    CART_ITEM_NOT_FOUND(4019, "购物车商品不存在"),
    STOCK_DEDUCT_FAIL(4020, "库存预扣失败"),
    IDEMPOTENT_KEY_TOO_LONG(4021, "幂等键长度超限（最大128字符）");

    private final Integer code;
    private final String message;
}
