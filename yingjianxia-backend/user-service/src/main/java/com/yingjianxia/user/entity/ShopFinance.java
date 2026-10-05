package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 店铺财务设置实体 — shop_finance 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_finance")
public class ShopFinance extends BaseEntity {

    /** 关联店铺 */
    private Long shopId;

    /** 开户名 */
    private String bankAccountName;

    /** 银行账号 */
    private String bankAccountNo;

    /** 开户行 */
    private String bankName;

    /** 提现阈值（元） */
    private BigDecimal withdrawThreshold;

    /** 提现费率（如 0.006 表示 0.6%） */
    private BigDecimal withdrawFeeRate;

    /** 结算周期：daily / weekly / monthly */
    private String settlementCycle;
}
