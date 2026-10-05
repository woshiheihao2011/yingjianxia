package com.yingjianxia.escrow.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 担保资金域错误码（5000~5999）
 */
@Getter
@AllArgsConstructor
public enum EscrowErrorCode implements IErrorCode {
    WALLET_NOT_FOUND(5001, "钱包不存在"),
    INSUFFICIENT_BALANCE(5002, "余额不足"),
    WALLET_ALREADY_EXISTS(5003, "钱包已存在，不能重复初始化"),
    OPTIMISTIC_LOCK_CONFLICT(5004, "并发扣款冲突，请重试"),
    ESCROW_RECORD_NOT_FOUND(5005, "担保记录不存在"),
    ESCROW_RECORD_ALREADY_EXISTS(5006, "该订单担保记录已存在"),
    ESCROW_STATUS_INVALID(5007, "担保记录状态不允许此操作"),
    RECHARGE_NOT_FOUND(5008, "充值记录不存在"),
    RECHARGE_ALREADY_SUCCESS(5009, "充值已成功，不能重复入账"),
    WITHDRAW_NOT_FOUND(5010, "提现记录不存在"),
    WITHDRAW_AMOUNT_INVALID(5011, "提现金额超限"),
    WITHDRAW_ALREADY_DONE(5012, "提现已处理完成"),
    TRANSACTION_DUPLICATE(5013, "交易流水重复（幂等键已存在）"),
    FREEZE_AMOUNT_INVALID(5014, "冻结金额不合法"),
    RELEASE_AMOUNT_MISMATCH(5015, "放款金额与冻结金额不一致"),
    REFUND_AMOUNT_EXCEED(5016, "退款金额超过冻结金额"),
    FEE_RATE_INVALID(5017, "手续费率配置异常"),
    RECHARGE_AMOUNT_INVALID(5018, "充值金额不合法"),
    TRANSACTION_NOT_FOUND(5019, "交易流水不存在"),
    LOCK_OPERATION_FAIL(5020, "钱包操作加锁失败");

    private final Integer code;
    private final String message;
}
