package com.yingjianxia.escrow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.escrow.dto.EscrowFreezeReq;
import com.yingjianxia.escrow.dto.EscrowReleaseReq;
import com.yingjianxia.escrow.dto.RechargeReq;
import com.yingjianxia.escrow.dto.RefundReq;
import com.yingjianxia.escrow.dto.TransactionQueryReq;
import com.yingjianxia.escrow.dto.WalletResp;
import com.yingjianxia.escrow.dto.WithdrawReq;
import com.yingjianxia.escrow.entity.EscrowRecord;
import com.yingjianxia.escrow.entity.RechargeRecord;
import com.yingjianxia.escrow.entity.Transaction;
import com.yingjianxia.escrow.entity.WithdrawalRecord;

/**
 * 担保资金服务接口
 * <p>
 * 核心：钱包初始化 / 充值 / T+1提现 / 担保冻结 / 担保放款 / 退款 / 全链路流水 / 乐观锁并发控制
 */
public interface EscrowService {

    /* ========== 钱包 ========== */
    /** 钱包初始化（一人一钱包，注册时调用） */
    Long initWallet(Long userId);

    /** 钱包查询 */
    WalletResp getWallet(Long userId);

    /* ========== 充值 ========== */
    /** 创建充值单（→待支付） */
    RechargeRecord createRecharge(RechargeReq req, Long userId);

    /** 充值回调确认（→入账 + 流水） */
    void rechargeCallback(String rechargeNo, String channelOrder, String idempotencyKey);

    /* ========== 提现 ========== */
    /** 提现申请（校验余额→冻结→T+1到账→流水） */
    WithdrawalRecord withdraw(WithdrawReq req, Long userId);

    /** 提现到账回调（T+1，从冻结扣减并标记成功） */
    void withdrawCallback(String withdrawNo, String idempotencyKey, boolean success, String reason);

    /* ========== 担保 ========== */
    /** 担保冻结（下单时冻结买家资金） */
    EscrowRecord escrowFreeze(EscrowFreezeReq req);

    /** 担保放款（确认收货时放款给卖家，扣除手续费） */
    EscrowRecord escrowRelease(EscrowReleaseReq req);

    /** 退款（取消/售后退款，解冻资金退回买家） */
    EscrowRecord refund(RefundReq req);

    /* ========== 流水查询 ========== */
    /** 交易流水查询（幂等键防重） */
    IPage<Transaction> listTransactions(Long userId, TransactionQueryReq req);

    /** 担保交易记录（买家或卖家视角） */
    IPage<EscrowRecord> listEscrowRecords(Long userId, Integer pageNum, Integer pageSize);

    /** 提现记录 */
    IPage<WithdrawalRecord> listWithdrawals(Long userId, Integer pageNum, Integer pageSize);
}
