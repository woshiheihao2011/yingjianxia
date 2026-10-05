package com.yingjianxia.payment.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付域错误码（6001~6015）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements IErrorCode {

    PAYMENT_ORDER_NOT_FOUND(6001, "支付单不存在"),
    CHANNEL_NOT_SUPPORTED(6002, "不支持的支付渠道"),
    PAYMENT_EXPIRED(6003, "支付单已过期，请重新发起支付"),
    CALLBACK_SIGN_VERIFY_FAIL(6004, "支付回调验签失败"),
    CALLBACK_DUPLICATE(6005, "重复的支付回调（已处理）"),
    PAYMENT_AMOUNT_MISMATCH(6006, "回调金额与支付单不匹配"),
    PAYMENT_ORDER_ALREADY_PAID(6007, "支付单已支付，不可重复支付"),
    PAYMENT_ORDER_CLOSED(6008, "支付单已关闭"),
    PAYMENT_ORDER_REFUNDED(6009, "支付单已退款"),
    UNIFIED_ORDER_FAIL(6010, "第三方统一下单失败"),
    PAYMENT_STATUS_INVALID(6011, "支付单状态不允许此操作"),
    REFUND_AMOUNT_EXCEED(6012, "退款金额超过原支付金额"),
    REFUND_CALLBACK_DUPLICATE(6013, "重复的退款回调（已处理）"),
    IDEMPOTENT_KEY_DUPLICATE(6014, "幂等键重复，请勿重复提交支付请求"),
    PAYMENT_QUERY_FAIL(6015, "主动查询支付状态失败");

    private final Integer code;
    private final String message;
}
