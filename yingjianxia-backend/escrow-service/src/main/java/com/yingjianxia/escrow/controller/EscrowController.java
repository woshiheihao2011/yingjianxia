package com.yingjianxia.escrow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
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
import com.yingjianxia.escrow.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

/**
 * 担保资金服务 Controller
 */
@Tag(name = "担保资金服务", description = "钱包/充值/提现/担保冻结放款退款/全链路流水")
@RestController
@RequestMapping({"/api/v1/escrow", "/api/v1/wallets"})
@RequiredArgsConstructor
public class EscrowController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final EscrowService service;

    /* ========== 钱包 ========== */

    @Operation(summary = "[用户] 查询我的钱包")
    @GetMapping({"/wallet", "/me"})
    public ApiResponse<WalletResp> wallet() {
        return ApiResponse.success(service.getWallet(UserContext.requiredUserId()));
    }

    @Operation(summary = "[用户] 交易流水查询")
    @GetMapping("/transactions")
    public ApiResponse<IPage<Transaction>> transactions(TransactionQueryReq req) {
        return ApiResponse.success(service.listTransactions(UserContext.requiredUserId(), req));
    }

    @Operation(summary = "[用户] 担保交易记录")
    @GetMapping("/records")
    public ApiResponse<IPage<EscrowRecord>> escrowRecords(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(service.listEscrowRecords(UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "[用户] 提现记录")
    @GetMapping("/withdrawals")
    public ApiResponse<IPage<WithdrawalRecord>> withdrawals(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(service.listWithdrawals(UserContext.requiredUserId(), pageNum, pageSize));
    }

    /* ========== 充值 ========== */

    @Operation(summary = "[用户] 创建充值单（→待支付）")
    @PostMapping("/recharge")
    public ApiResponse<RechargeRecord> createRecharge(@Valid @RequestBody RechargeReq req) {
        return ApiResponse.success(service.createRecharge(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[内部] 充值回调确认（第三方支付回调）", hidden = true)
    @PostMapping("/internal/recharge/callback")
    public ApiResponse<Void> rechargeCallback(@RequestParam String rechargeNo,
                                               @RequestParam(required = false) String channelOrder,
                                               @RequestParam(required = false) String idempotencyKey,
                                               @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.rechargeCallback(rechargeNo, channelOrder, idempotencyKey);
        return ApiResponse.success();
    }

    /* ========== 提现 ========== */

    @Operation(summary = "[用户] 提现申请（T+1到账）")
    @PostMapping("/withdraw")
    public ApiResponse<WithdrawalRecord> withdraw(@Valid @RequestBody WithdrawReq req) {
        return ApiResponse.success(service.withdraw(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[内部] 提现到账回调", hidden = true)
    @PostMapping("/internal/withdraw/callback")
    public ApiResponse<Void> withdrawCallback(@RequestParam String withdrawNo,
                                              @RequestParam(required = false) String idempotencyKey,
                                              @RequestParam boolean success,
                                              @RequestParam(required = false) String reason,
                                              @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.withdrawCallback(withdrawNo, idempotencyKey, success, reason);
        return ApiResponse.success();
    }

    /* ========== [内部] 担保冻结/放款/退款 ========== */

    @Operation(summary = "[内部] 担保冻结（下单时冻结买家资金）", hidden = true)
    @PostMapping("/internal/freeze")
    public ApiResponse<EscrowRecord> freeze(@Valid @RequestBody EscrowFreezeReq req,
                                            @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(service.escrowFreeze(req));
    }

    @Operation(summary = "[内部] 担保放款（确认收货放款给卖家）", hidden = true)
    @PostMapping("/internal/release")
    public ApiResponse<EscrowRecord> release(@Valid @RequestBody EscrowReleaseReq req,
                                             @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(service.escrowRelease(req));
    }

    @Operation(summary = "[内部] 退款（取消/售后退款）", hidden = true)
    @PostMapping("/internal/refund")
    public ApiResponse<EscrowRecord> refund(@Valid @RequestBody RefundReq req,
                                            @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(service.refund(req));
    }

    @Operation(summary = "[内部] 钱包初始化（注册时调用）", hidden = true)
    @PostMapping("/internal/wallet/init")
    public ApiResponse<Long> initWallet(@RequestParam Long userId,
                                       @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        return ApiResponse.success(service.initWallet(userId));
    }
}
