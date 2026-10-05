package com.yingjianxia.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 验机检测项明细 — inspection_items
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_items")
public class InspectionItem extends BaseEntity {

    private Long reportId;
    private Integer itemNo;
    private String itemName;

    /** 通过/通过率百分比/具体数值等 */
    private String result;

    /** 是否通过 */
    private Boolean isPassed;

    /** JSON 检测详情 */
    private String detail;

    /* ========== deleted/version/createdAt/updatedAt 由 BaseEntity 继承，表已有对应列 ========== */
}
