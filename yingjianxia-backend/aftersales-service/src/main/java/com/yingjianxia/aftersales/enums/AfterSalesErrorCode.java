package com.yingjianxia.aftersales.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后域错误码（9001~9020）
 */
@Getter
@AllArgsConstructor
public enum AfterSalesErrorCode implements IErrorCode {
    AFTER_SALE_NOT_FOUND(9001, "售后单不存在"),
    INVALID_STATUS_TRANSITION(9002, "售后状态流转非法，当前状态不允许该操作"),
    REFUND_AMOUNT_EXCEED(9003, "退款金额超出订单实付金额"),
    DUPLICATE_APPLY(9004, "该订单明细已存在进行中的售后申请，不能重复申请"),
    APPLY_TIMEOUT(9005, "已超过7天退换期限，不可申请售后"),
    NO_PERMISSION(9006, "无权操作该售后单"),
    SELLER_ALREADY_HANDLE(9007, "卖家已处理，不可重复操作"),
    RETURN_SHIPMENT_NOT_FILLED(9008, "买家尚未填写退货物流，无法确认收货"),
    ARBITRATION_ALREADY_EXISTS(9009, "该售后单已存在仲裁记录，不可重复仲裁"),
    ARBITRATION_NOT_APPLIED(9010, "买家未申请仲裁，卖家不可发起仲裁流程"),
    EVIDENCE_IMAGES_EXCEED(9011, "凭证图片最多6张"),
    REFUND_AMOUNT_INVALID(9012, "退款金额必须大于0"),
    ORDER_ITEM_REQUIRED(9013, "换货类型必须指定订单明细"),
    MESSAGE_EMPTY(9014, "沟通内容不能为空"),
    TRACKING_NO_REQUIRED(9015, "退货运单号不能为空"),
    SELLER_RESPONSE_REQUIRED(9016, "卖家处理结果不能为空"),
    ARBITRATION_RESULT_INVALID(9017, "仲裁结果参数无效"),
    ARBITRATION_REFUND_INVALID(9018, "部分退款必须填写退款金额"),
    AUTO_AGREE_FAIL(9019, "超时自动同意处理失败"),
    ESCROW_REFUND_FAIL(9020, "触发退款服务失败，请稍后重试");

    private final Integer code;
    private final String message;
}
