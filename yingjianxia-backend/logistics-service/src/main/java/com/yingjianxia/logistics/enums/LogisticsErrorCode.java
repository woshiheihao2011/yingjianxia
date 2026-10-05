package com.yingjianxia.logistics.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 物流域错误码（8001~8010）
 */
@Getter
@AllArgsConstructor
public enum LogisticsErrorCode implements IErrorCode {
    SHIPMENT_NOT_FOUND(8001, "发货记录不存在"),
    TRACKING_NO_DUPLICATED(8002, "运单号已存在，不能重复发货"),
    TRACK_NOT_FOUND(8003, "物流轨迹不存在"),
    INVALID_STATUS_TRANSITION(8004, "发货状态流转不合法"),
    NO_PERMISSION(8005, "无权操作该发货记录"),
    SIGN_VERIFY_FAIL(8006, "物流回调签名校验失败"),
    TRACKING_NO_REQUIRED(8007, "运单号不能为空"),
    ORDER_ITEM_IDS_EMPTY(8008, "发货商品明细不能为空"),
    EXPRESS_CODE_INVALID(8009, "快递公司编码无效"),
    SHIPMENT_ALREADY_RECEIVED(8010, "发货记录已签收，不可重复操作");

    private final Integer code;
    private final String message;
}
