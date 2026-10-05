package com.yingjianxia.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 验机报告 — inspection_reports 表
 * <p>
 * 硬约束：signature 字段 SHA-256，报告一旦"已完成"不可被业务层 UPDATE。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_reports")
public class InspectionReport extends BaseEntity {

    /** 关联商品 */
    private Long productId;

    /** 关联检测订单/入库单号 */
    private Long orderId;

    /** 评级：优/良/合格/不合格 */
    private String grade;

    /** 1全部通过 2部分通过 3不通过 */
    private Integer overallResult;

    /** SHA-256 数字签名（防篡改核心字段） */
    private String signature;

    /** 完整报告 PDF URL（OSS/CDN） */
    private String reportFileUrl;

    /** 检测员ID */
    private Long inspectorId;

    /** 检测员姓名（冗余） */
    private String inspectorName;

    /** 0待检测 1检测中 2已完成 3异常 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 收货时间 */
    private LocalDateTime receivedAt;

    /** 报告生成时间 */
    private LocalDateTime completedAt;

    /* ========== deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 非持久化：检测项列表 ========== */
    @TableField(exist = false)
    private List<InspectionItem> items;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_TESTING = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_ERROR = 3;

    /* ========== 评级常量 ========== */
    public static final String GRADE_EXCELLENT = "优";
    public static final String GRADE_GOOD = "良";
    public static final String GRADE_PASS = "合格";
    public static final String GRADE_FAIL = "不合格";

    public static final int RESULT_ALL_PASS = 1;
    public static final int RESULT_PART_PASS = 2;
    public static final int RESULT_FAIL = 3;
}
