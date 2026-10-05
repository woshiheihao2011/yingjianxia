package com.yingjianxia.aftersales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台仲裁记录 — arbitration_records 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("arbitration_records")
public class ArbitrationRecord extends BaseEntity {

    /** 关联售后单 */
    private Long afterSaleId;

    /** 仲裁员（平台运营） */
    private Long arbitratorId;

    /** 仲裁员姓名 */
    private String arbitratorName;

    /** 仲裁结果：1支持买家(退款) 2支持卖家 3部分退款 */
    private Integer result;

    /** 裁定退款金额 */
    private BigDecimal refundAmount;

    /** 仲裁理由 */
    private String reason;

    /** 证据摘要 */
    private String evidenceSummary;

    /* ========== 本表不存在：deleted/version/updated_at ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;
    @TableField(exist = false) private LocalDateTime updatedAt;

    /* ========== 结果常量 ========== */
    public static final int RESULT_SUPPORT_BUYER = 1;
    public static final int RESULT_SUPPORT_SELLER = 2;
    public static final int RESULT_PARTIAL_REFUND = 3;
}
