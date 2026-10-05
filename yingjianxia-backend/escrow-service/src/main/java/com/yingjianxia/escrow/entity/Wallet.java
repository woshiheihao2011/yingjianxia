package com.yingjianxia.escrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钱包表 — wallets
 * <p>
 * 一人一钱包。version 字段为乐观锁，防并发扣款。
 * total_balance = available_balance + frozen_balance
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wallets")
public class Wallet extends BaseEntity {

    /** 用户ID（一人一钱包） */
    private Long userId;

    /** 总余额 = 可用 + 冻结 */
    private BigDecimal totalBalance;

    /** 可用余额 */
    private BigDecimal availableBalance;

    /** 冻结金额（担保中） */
    private BigDecimal frozenBalance;

    /** 最后一笔流水号 */
    private String lastTxNo;

    /* ========== 本表不存在：deleted ========== */
    // version 字段存在，沿用 BaseEntity 的乐观锁 @Version
    @TableField(exist = false) private Integer deleted;
}
