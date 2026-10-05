package com.yingjianxia.escrow.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.exception.BusinessException;
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
import com.yingjianxia.escrow.entity.Wallet;
import com.yingjianxia.escrow.entity.WalletLockLog;
import com.yingjianxia.escrow.entity.WithdrawalRecord;
import com.yingjianxia.escrow.enums.EscrowErrorCode;
import com.yingjianxia.escrow.mapper.EscrowRecordMapper;
import com.yingjianxia.escrow.mapper.RechargeRecordMapper;
import com.yingjianxia.escrow.mapper.TransactionMapper;
import com.yingjianxia.escrow.mapper.WalletLockLogMapper;
import com.yingjianxia.escrow.mapper.WalletMapper;
import com.yingjianxia.escrow.mapper.WithdrawalRecordMapper;
import com.yingjianxia.escrow.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * 担保资金服务实现
 * <p>
 * 核心原则：资金安全第一，全链路审计。
 * - 资金操作必有幂等键（idempotency_key）校验
 * - 钱包操作用乐观锁 version 字段（失败重试3次）
 * - 担保状态机：0待冻结→1已冻结→2已放款/3已退款/4退款中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private final WalletMapper walletMapper;
    private final EscrowRecordMapper escrowRecordMapper;
    private final TransactionMapper transactionMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final WithdrawalRecordMapper withdrawalRecordMapper;
    private final WalletLockLogMapper walletLockLogMapper;

    @Value("${yingjianxia.escrow.fee-rate:1.00}")
    private BigDecimal feeRate;

    @Value("${yingjianxia.escrow.withdrawal-fee-rate:0.00}")
    private BigDecimal withdrawFeeRate;

    @Value("${yingjianxia.escrow.min-withdrawal:1.00}")
    private BigDecimal minWithdrawal;

    @Value("${yingjianxia.escrow.max-withdrawal:50000.00}")
    private BigDecimal maxWithdrawal;

    private static final int OPTIMISTIC_RETRY = 3;

    /* ============================ 钱包 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long initWallet(Long userId) {
        Long exists = walletMapper.selectCount(new LambdaQueryWrapper<Wallet>()
                .eq(Wallet::getUserId, userId));
        if (exists != null && exists > 0) {
            throw new BusinessException(EscrowErrorCode.WALLET_ALREADY_EXISTS);
        }
        Wallet w = new Wallet();
        w.setUserId(userId);
        w.setTotalBalance(BigDecimal.ZERO);
        w.setAvailableBalance(BigDecimal.ZERO);
        w.setFrozenBalance(BigDecimal.ZERO);
        walletMapper.insert(w);
        log.info("【钱包初始化】userId={}, walletId={}", userId, w.getId());
        return w.getId();
    }

    @Override
    public WalletResp getWallet(Long userId) {
        Wallet w = requireWallet(userId);
        WalletResp resp = new WalletResp();
        resp.setId(w.getId());
        resp.setUserId(w.getUserId());
        resp.setTotalBalance(w.getTotalBalance());
        resp.setAvailableBalance(w.getAvailableBalance());
        resp.setFrozenBalance(w.getFrozenBalance());
        resp.setLastTxNo(w.getLastTxNo());
        resp.setCreatedAt(w.getCreatedAt());
        resp.setUpdatedAt(w.getUpdatedAt());
        return resp;
    }

    /* ============================ 充值 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecord createRecharge(RechargeReq req, Long userId) {
        requireWallet(userId);
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(EscrowErrorCode.RECHARGE_AMOUNT_INVALID);
        }
        RechargeRecord r = new RechargeRecord();
        r.setRechargeNo(generateNo("RC"));
        r.setUserId(userId);
        r.setAmount(req.getAmount());
        r.setChannel(req.getChannel());
        r.setStatus(RechargeRecord.STATUS_PENDING);
        r.setIdempotencyKey(StrUtil.isBlank(req.getIdempotencyKey()) ? generateNo("IK") : req.getIdempotencyKey());
        rechargeRecordMapper.insert(r);
        log.info("【创建充值单】rechargeNo={}, userId={}, amount={}", r.getRechargeNo(), userId, req.getAmount());
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargeCallback(String rechargeNo, String channelOrder, String idempotencyKey) {
        RechargeRecord r = rechargeRecordMapper.selectOne(new LambdaQueryWrapper<RechargeRecord>()
                .eq(RechargeRecord::getRechargeNo, rechargeNo));
        if (r == null) throw new BusinessException(EscrowErrorCode.RECHARGE_NOT_FOUND);
        if (r.getStatus() == RechargeRecord.STATUS_SUCCESS) {
            throw new BusinessException(EscrowErrorCode.RECHARGE_ALREADY_SUCCESS);
        }
        LocalDateTime now = LocalDateTime.now();
        // 入账：available += amount, total += amount
        BalanceSnapshot snap = updateWalletOptimistic(r.getUserId(), w -> {
            w.setAvailableBalance(w.getAvailableBalance().add(r.getAmount()));
            w.setTotalBalance(w.getTotalBalance().add(r.getAmount()));
        });
        // 更新充值单
        rechargeRecordMapper.update(null, new LambdaUpdateWrapper<RechargeRecord>()
                .eq(RechargeRecord::getId, r.getId())
                .set(RechargeRecord::getStatus, RechargeRecord.STATUS_SUCCESS)
                .set(RechargeRecord::getChannelOrder, channelOrder)
                .set(RechargeRecord::getPaidAt, now));
        // 流水（幂等键防重）
        recordTransaction(r.getUserId(), Transaction.TYPE_RECHARGE, Transaction.DIRECTION_IN,
                r.getAmount(), snap, null, null, null, r.getChannel(), "recharge", BigDecimal.ZERO,
                "充值入账", idempotencyKey);
        log.info("【充值回调入账】rechargeNo={}, userId={}, amount={}", rechargeNo, r.getUserId(), r.getAmount());
    }

    /* ============================ 提现 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRecord withdraw(WithdrawReq req, Long userId) {
        Wallet w = requireWallet(userId);
        if (req.getAmount() == null || req.getAmount().compareTo(minWithdrawal) < 0
                || req.getAmount().compareTo(maxWithdrawal) > 0) {
            throw new BusinessException(EscrowErrorCode.WITHDRAW_AMOUNT_INVALID);
        }
        if (w.getAvailableBalance().compareTo(req.getAmount()) < 0) {
            throw new BusinessException(EscrowErrorCode.INSUFFICIENT_BALANCE);
        }
        BigDecimal fee = req.getAmount().multiply(withdrawFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal actual = req.getAmount().subtract(fee);
        LocalDateTime now = LocalDateTime.now();

        // 冻结：available -= amount, frozen += amount（total不变）
        BalanceSnapshot snap = updateWalletOptimistic(userId, wx -> {
            if (wx.getAvailableBalance().compareTo(req.getAmount()) < 0) {
                throw new BusinessException(EscrowErrorCode.INSUFFICIENT_BALANCE);
            }
            wx.setAvailableBalance(wx.getAvailableBalance().subtract(req.getAmount()));
            wx.setFrozenBalance(wx.getFrozenBalance().add(req.getAmount()));
        });
        recordWalletLockLog(userId, null, WalletLockLog.LOCK_FREEZE, req.getAmount(), "提现冻结");

        WithdrawalRecord r = new WithdrawalRecord();
        r.setWithdrawNo(generateNo("WD"));
        r.setUserId(userId);
        r.setAmount(req.getAmount());
        r.setFee(fee);
        r.setActualAmount(actual);
        r.setChannel(req.getChannel());
        r.setTargetAccount(req.getTargetAccount());
        r.setTargetName(req.getTargetName());
        r.setStatus(WithdrawalRecord.STATUS_PROCESSING);
        r.setArriveAt(now.plusDays(1));
        r.setIdempotencyKey(StrUtil.isBlank(req.getIdempotencyKey()) ? generateNo("IK") : req.getIdempotencyKey());
        withdrawalRecordMapper.insert(r);
        // 流水：冻结
        recordTransaction(userId, Transaction.TYPE_FREEZE, Transaction.DIRECTION_OUT,
                req.getAmount(), snap, null, null, null, req.getChannel(), "withdraw_freeze",
                fee, "提现申请冻结资金", r.getIdempotencyKey());
        log.info("【提现申请】withdrawNo={}, userId={}, amount={}, arriveAt={}", r.getWithdrawNo(), userId, req.getAmount(), r.getArriveAt());
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawCallback(String withdrawNo, String idempotencyKey, boolean success, String reason) {
        WithdrawalRecord r = withdrawalRecordMapper.selectOne(new LambdaQueryWrapper<WithdrawalRecord>()
                .eq(WithdrawalRecord::getWithdrawNo, withdrawNo));
        if (r == null) throw new BusinessException(EscrowErrorCode.WITHDRAW_NOT_FOUND);
        if (r.getStatus() != WithdrawalRecord.STATUS_PROCESSING) {
            throw new BusinessException(EscrowErrorCode.WITHDRAW_ALREADY_DONE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (success) {
            // 到账：frozen -= amount, total -= amount
            BalanceSnapshot snap = updateWalletOptimistic(r.getUserId(), w -> {
                w.setFrozenBalance(w.getFrozenBalance().subtract(r.getAmount()));
                w.setTotalBalance(w.getTotalBalance().subtract(r.getAmount()));
            });
            recordWalletLockLog(r.getUserId(), null, WalletLockLog.LOCK_UNFREEZE, r.getAmount(), "提现到账扣减");
            withdrawalRecordMapper.update(null, new LambdaUpdateWrapper<WithdrawalRecord>()
                    .eq(WithdrawalRecord::getId, r.getId())
                    .set(WithdrawalRecord::getStatus, WithdrawalRecord.STATUS_SUCCESS)
                    .set(WithdrawalRecord::getSuccessAt, now));
            recordTransaction(r.getUserId(), Transaction.TYPE_WITHDRAW, Transaction.DIRECTION_OUT,
                    r.getAmount(), snap, null, null, null, r.getChannel(), "withdraw_arrive",
                    r.getFee(), "提现到账", idempotencyKey);
        } else {
            // 失败：解冻回退 available += amount, frozen -= amount
            BalanceSnapshot snap = updateWalletOptimistic(r.getUserId(), w -> {
                w.setAvailableBalance(w.getAvailableBalance().add(r.getAmount()));
                w.setFrozenBalance(w.getFrozenBalance().subtract(r.getAmount()));
            });
            recordWalletLockLog(r.getUserId(), null, WalletLockLog.LOCK_UNFREEZE, r.getAmount(), "提现失败回退");
            withdrawalRecordMapper.update(null, new LambdaUpdateWrapper<WithdrawalRecord>()
                    .eq(WithdrawalRecord::getId, r.getId())
                    .set(WithdrawalRecord::getStatus, WithdrawalRecord.STATUS_FAIL)
                    .set(WithdrawalRecord::getFailedReason, reason));
            recordTransaction(r.getUserId(), Transaction.TYPE_UNFREEZE, Transaction.DIRECTION_IN,
                    r.getAmount(), snap, null, null, null, r.getChannel(), "withdraw_fail_refund",
                    BigDecimal.ZERO, "提现失败资金回退", idempotencyKey);
        }
        log.info("【提现回调】withdrawNo={}, success={}", withdrawNo, success);
    }

    /* ============================ 担保 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EscrowRecord escrowFreeze(EscrowFreezeReq req) {
        // 幂等键校验（uk_idempotency_key）
        if (StrUtil.isNotBlank(req.getIdempotencyKey())) {
            Long dup = escrowRecordMapper.selectCount(new LambdaQueryWrapper<EscrowRecord>()
                    .eq(EscrowRecord::getIdempotencyKey, req.getIdempotencyKey()));
            if (dup != null && dup > 0) {
                throw new BusinessException(EscrowErrorCode.ESCROW_RECORD_ALREADY_EXISTS);
            }
        }
        // 一订单一担保（uk_order_id）
        Long existOrder = escrowRecordMapper.selectCount(new LambdaQueryWrapper<EscrowRecord>()
                .eq(EscrowRecord::getOrderId, req.getOrderId()));
        if (existOrder != null && existOrder > 0) {
            throw new BusinessException(EscrowErrorCode.ESCROW_RECORD_ALREADY_EXISTS);
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(EscrowErrorCode.FREEZE_AMOUNT_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        // 买家冻结：available -= amount, frozen += amount
        BalanceSnapshot snap = updateWalletOptimistic(req.getBuyerId(), w -> {
            if (w.getAvailableBalance().compareTo(req.getAmount()) < 0) {
                throw new BusinessException(EscrowErrorCode.INSUFFICIENT_BALANCE);
            }
            w.setAvailableBalance(w.getAvailableBalance().subtract(req.getAmount()));
            w.setFrozenBalance(w.getFrozenBalance().add(req.getAmount()));
        });
        recordWalletLockLog(req.getBuyerId(), null, WalletLockLog.LOCK_FREEZE, req.getAmount(), "担保冻结");

        // 写担保记录
        EscrowRecord esc = new EscrowRecord();
        esc.setOrderId(req.getOrderId());
        esc.setOrderNo(req.getOrderNo());
        esc.setBuyerId(req.getBuyerId());
        esc.setSellerId(req.getSellerId());
        esc.setAmount(req.getAmount());
        esc.setFeeRate(feeRate);
        esc.setFee(BigDecimal.ZERO); // 冻结阶段手续费为0，放款时计算
        esc.setSettleAmount(BigDecimal.ZERO);
        esc.setStatus(EscrowRecord.STATUS_FROZEN);
        esc.setPaymentMethod(req.getPaymentMethod());
        esc.setPaymentChannelTx(req.getPaymentChannelTx());
        esc.setFrozenAt(now);
        esc.setIdempotencyKey(req.getIdempotencyKey());
        escrowRecordMapper.insert(esc);

        // 流水（买家冻结）
        recordTransaction(req.getBuyerId(), Transaction.TYPE_FREEZE, Transaction.DIRECTION_OUT,
                req.getAmount(), snap, req.getOrderId(), esc.getId(), req.getSellerId(),
                req.getPaymentMethod(), "order_freeze", BigDecimal.ZERO, "下单担保冻结资金", req.getIdempotencyKey());
        log.info("【担保冻结】orderId={}, buyerId={}, amount={}", req.getOrderId(), req.getBuyerId(), req.getAmount());
        return esc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EscrowRecord escrowRelease(EscrowReleaseReq req) {
        EscrowRecord esc = requireEscrowByOrder(req.getOrderId());
        if (esc.getStatus() != EscrowRecord.STATUS_FROZEN) {
            throw new BusinessException(EscrowErrorCode.ESCROW_STATUS_INVALID);
        }
        // 幂等键校验（放款幂等键与冻结键不同，防重复放款）
        if (StrUtil.isNotBlank(req.getIdempotencyKey())) {
            Long dup = transactionMapper.selectCount(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getIdempotencyKey, req.getIdempotencyKey())
                    .eq(Transaction::getEscrowId, esc.getId()));
            if (dup != null && dup > 0) {
                throw new BusinessException(EscrowErrorCode.TRANSACTION_DUPLICATE);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        // 手续费 fee = amount * feeRate / 100, settle = amount - fee
        BigDecimal fee = esc.getAmount().multiply(feeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal settle = esc.getAmount().subtract(fee);

        // 买家：frozen -= amount, total -= amount（资金离开买家）
        BalanceSnapshot buyerSnap = updateWalletOptimistic(esc.getBuyerId(), w -> {
            if (w.getFrozenBalance().compareTo(esc.getAmount()) < 0) {
                throw new BusinessException(EscrowErrorCode.REFUND_AMOUNT_EXCEED);
            }
            w.setFrozenBalance(w.getFrozenBalance().subtract(esc.getAmount()));
            w.setTotalBalance(w.getTotalBalance().subtract(esc.getAmount()));
        });
        recordWalletLockLog(esc.getBuyerId(), null, WalletLockLog.LOCK_UNFREEZE, esc.getAmount(), "担保放款扣减");

        // 卖家：available += settle, total += settle
        BalanceSnapshot sellerSnap = updateWalletOptimistic(esc.getSellerId(), w -> {
            w.setAvailableBalance(w.getAvailableBalance().add(settle));
            w.setTotalBalance(w.getTotalBalance().add(settle));
        });

        // 更新担保记录
        escrowRecordMapper.update(null, new LambdaUpdateWrapper<EscrowRecord>()
                .eq(EscrowRecord::getId, esc.getId())
                .set(EscrowRecord::getStatus, EscrowRecord.STATUS_RELEASED)
                .set(EscrowRecord::getFee, fee)
                .set(EscrowRecord::getSettleAmount, settle)
                .set(EscrowRecord::getReleasedAt, now));

        // 流水：买家解冻（支出），卖家收入
        recordTransaction(esc.getBuyerId(), Transaction.TYPE_UNFREEZE, Transaction.DIRECTION_OUT,
                esc.getAmount(), buyerSnap, esc.getOrderId(), esc.getId(), esc.getSellerId(),
                esc.getPaymentMethod(), "escrow_release", fee, "担保放款-买家资金划出", req.getIdempotencyKey());
        recordTransaction(esc.getSellerId(), Transaction.TYPE_INCOME, Transaction.DIRECTION_IN,
                settle, sellerSnap, esc.getOrderId(), esc.getId(), esc.getBuyerId(),
                esc.getPaymentMethod(), "escrow_settle", BigDecimal.ZERO, "担保放款-卖家到账", null);
        // TODO: 平台手续费 fee 入平台钱包
        log.info("【担保放款】orderId={}, buyerId={}, sellerId={}, amount={}, fee={}, settle={}",
                req.getOrderId(), esc.getBuyerId(), esc.getSellerId(), esc.getAmount(), fee, settle);
        return escrowRecordMapper.selectById(esc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EscrowRecord refund(RefundReq req) {
        EscrowRecord esc = requireEscrowByOrder(req.getOrderId());
        if (esc.getStatus() != EscrowRecord.STATUS_FROZEN) {
            throw new BusinessException(EscrowErrorCode.ESCROW_STATUS_INVALID);
        }
        if (StrUtil.isNotBlank(req.getIdempotencyKey())) {
            Long dup = transactionMapper.selectCount(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getIdempotencyKey, req.getIdempotencyKey())
                    .eq(Transaction::getEscrowId, esc.getId()));
            if (dup != null && dup > 0) {
                throw new BusinessException(EscrowErrorCode.TRANSACTION_DUPLICATE);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        // 退款：frozen -= amount, available += amount（资金退回买家可用，total不变）
        BalanceSnapshot snap = updateWalletOptimistic(esc.getBuyerId(), w -> {
            if (w.getFrozenBalance().compareTo(esc.getAmount()) < 0) {
                throw new BusinessException(EscrowErrorCode.REFUND_AMOUNT_EXCEED);
            }
            w.setFrozenBalance(w.getFrozenBalance().subtract(esc.getAmount()));
            w.setAvailableBalance(w.getAvailableBalance().add(esc.getAmount()));
        });
        recordWalletLockLog(esc.getBuyerId(), null, WalletLockLog.LOCK_UNFREEZE, esc.getAmount(), "担保退款解冻");

        escrowRecordMapper.update(null, new LambdaUpdateWrapper<EscrowRecord>()
                .eq(EscrowRecord::getId, esc.getId())
                .set(EscrowRecord::getStatus, EscrowRecord.STATUS_REFUNDED)
                .set(EscrowRecord::getReleasedAt, now)
                .set(EscrowRecord::getRefundReason, req.getReason())
                .set(EscrowRecord::getAfterSaleId, req.getAfterSaleId()));

        recordTransaction(esc.getBuyerId(), Transaction.TYPE_REFUND, Transaction.DIRECTION_IN,
                esc.getAmount(), snap, esc.getOrderId(), esc.getId(), null,
                esc.getPaymentMethod(), "escrow_refund", BigDecimal.ZERO,
                "担保退款-资金退回买家", req.getIdempotencyKey());
        log.info("【担保退款】orderId={}, buyerId={}, amount={}", req.getOrderId(), esc.getBuyerId(), esc.getAmount());
        return escrowRecordMapper.selectById(esc.getId());
    }

    /* ============================ 流水查询 ============================ */

    @Override
    public IPage<Transaction> listTransactions(Long userId, TransactionQueryReq req) {
        Page<Transaction> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<Transaction> w = new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getUserId, userId)
                .orderByDesc(Transaction::getCreatedAt);
        if (StrUtil.isNotBlank(req.getTxNo())) w.eq(Transaction::getTxNo, req.getTxNo());
        if (req.getType() != null) w.eq(Transaction::getType, req.getType());
        if (req.getOrderId() != null) w.eq(Transaction::getOrderId, req.getOrderId());
        return transactionMapper.selectPage(page, w);
    }

    @Override
    public IPage<EscrowRecord> listEscrowRecords(Long userId, Integer pageNum, Integer pageSize) {
        Page<EscrowRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EscrowRecord> w = new LambdaQueryWrapper<EscrowRecord>()
                .and(q -> q.eq(EscrowRecord::getBuyerId, userId).or().eq(EscrowRecord::getSellerId, userId))
                .orderByDesc(EscrowRecord::getCreatedAt);
        return escrowRecordMapper.selectPage(page, w);
    }

    @Override
    public IPage<WithdrawalRecord> listWithdrawals(Long userId, Integer pageNum, Integer pageSize) {
        Page<WithdrawalRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WithdrawalRecord> w = new LambdaQueryWrapper<WithdrawalRecord>()
                .eq(WithdrawalRecord::getUserId, userId)
                .orderByDesc(WithdrawalRecord::getCreatedAt);
        return withdrawalRecordMapper.selectPage(page, w);
    }

    /* ============================ 内部工具 ============================ */

    private Wallet requireWallet(Long userId) {
        Wallet w = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        if (w == null) {
            // 钱包不存在时自动创建（新用户首次访问）
            try {
                initWallet(userId);
            } catch (BusinessException e) {
                // WALLET_ALREADY_EXISTS 说明并发创建，忽略即可
                if (!e.getCode().equals(EscrowErrorCode.WALLET_ALREADY_EXISTS.getCode())) throw e;
            }
            w = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
            if (w == null) throw new BusinessException(EscrowErrorCode.WALLET_NOT_FOUND);
        }
        return w;
    }

    private EscrowRecord requireEscrowByOrder(Long orderId) {
        EscrowRecord esc = escrowRecordMapper.selectOne(new LambdaQueryWrapper<EscrowRecord>()
                .eq(EscrowRecord::getOrderId, orderId));
        if (esc == null) throw new BusinessException(EscrowErrorCode.ESCROW_RECORD_NOT_FOUND);
        return esc;
    }

    /**
     * 乐观锁更新钱包：重试 OPTIMISTIC_RETRY 次。
     * mutator 在加载的最新钱包上修改余额字段；updateById 自动带 version 校验，
     * 若返回0行（并发冲突）则重新加载重试；业务校验异常直接抛出不重试。
     *
     * @return 变更前后的余额快照（用于写流水 balance_before/after）
     */
    private BalanceSnapshot updateWalletOptimistic(Long userId, Consumer<Wallet> mutator) {
        for (int i = 0; i < OPTIMISTIC_RETRY; i++) {
            Wallet w = requireWallet(userId);
            BigDecimal availBefore = w.getAvailableBalance();
            BigDecimal frozenBefore = w.getFrozenBalance();
            BigDecimal totalBefore = w.getTotalBalance();
            mutator.accept(w); // 在最新数据上做校验+赋值，业务异常会直接抛出
            int rows = walletMapper.updateById(w); // @Version 自动乐观锁
            if (rows > 0) {
                return new BalanceSnapshot(availBefore, frozenBefore, totalBefore,
                        w.getAvailableBalance(), w.getFrozenBalance(), w.getTotalBalance());
            }
            log.warn("【钱包乐观锁冲突】userId={}, retry={}", userId, i + 1);
        }
        throw new BusinessException(EscrowErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    private void recordTransaction(Long userId, int type, int direction, BigDecimal amount,
                                   BalanceSnapshot snap, Long orderId, Long escrowId, Long counterpartyId,
                                   String paymentMethod, String subType, BigDecimal fee, String remark,
                                   String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.setTxNo(generateNo("TX"));
        tx.setUserId(userId);
        tx.setType(type);
        tx.setSubType(subType);
        tx.setAmount(amount);
        tx.setDirection(direction);
        tx.setBalanceBefore(snap.getAvailBefore());
        tx.setBalanceAfter(snap.getAvailAfter());
        tx.setFrozenBefore(snap.getFrozenBefore());
        tx.setFrozenAfter(snap.getFrozenAfter());
        tx.setOrderId(orderId);
        tx.setEscrowId(escrowId);
        tx.setCounterpartyId(counterpartyId);
        tx.setPaymentMethod(paymentMethod);
        tx.setFee(fee == null ? BigDecimal.ZERO : fee);
        tx.setRemark(remark);
        tx.setIdempotencyKey(idempotencyKey);
        transactionMapper.insert(tx);
        // 更新钱包 last_tx_no（信息字段，不走乐观锁，last-writer-wins）
        walletMapper.update(null, new LambdaUpdateWrapper<Wallet>()
                .eq(Wallet::getUserId, userId)
                .set(Wallet::getLastTxNo, tx.getTxNo()));
    }

    private void recordWalletLockLog(Long userId, String txNo, int lockType, BigDecimal amount, String reason) {
        WalletLockLog l = new WalletLockLog();
        l.setUserId(userId);
        l.setTxNo(txNo == null ? "" : txNo);
        l.setLockType(lockType);
        l.setAmount(amount);
        l.setReason(reason);
        walletLockLogMapper.insert(l);
    }

    private String generateNo(String prefix) {
        return prefix + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    /** 余额快照（变更前后） */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class BalanceSnapshot {
        private BigDecimal availBefore;
        private BigDecimal frozenBefore;
        private BigDecimal totalBefore;
        private BigDecimal availAfter;
        private BigDecimal frozenAfter;
        private BigDecimal totalAfter;
    }
}
