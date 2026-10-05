package com.yingjianxia.aftersales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 售后状态变更日志 — after_sale_status_logs 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale_status_logs")
public class AfterSaleStatusLog extends BaseEntity {

    /** 售后单ID */
    private Long afterSaleId;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 1买家 2卖家 3系统 4客服仲裁 */
    private Integer operatorType;

    /** 变更说明 */
    private String remark;

    /* ========== 本表不存在：deleted/version/updated_at ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;
    @TableField(exist = false) private LocalDateTime updatedAt;
}
