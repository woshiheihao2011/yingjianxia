package com.yingjianxia.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 审核记录 — audit_records 表（审核域自建表）
 * <p>
 * 目标类型 targetType：1商品 2帖子 3举报 4提现
 * 操作 action：1通过 2拒绝 3下架
 * 状态 status：0待审核 1已通过 2已拒绝
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_records")
public class AuditRecord extends BaseEntity {

    /** 目标类型：1商品 2帖子 3举报 4提现 */
    private Integer targetType;

    /** 目标对象ID */
    private Long targetId;

    /** 审核员ID */
    private Long auditorId;

    /** 审核员姓名（冗余） */
    private String auditorName;

    /** 操作：0待处理 1通过 2拒绝 3下架 */
    private Integer action;

    /** 审核理由 */
    private String reason;

    /** 审核意见 */
    private String comment;

    /** 证据附件（JSON） */
    private String evidence;

    /** 0待审核 1已通过 2已拒绝 */
    private Integer status;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 非持久化：证据列表（JSON解析后） ========== */
    @TableField(exist = false)
    private List<String> evidenceList;

    /* ========== 目标类型常量 ========== */
    public static final int TARGET_PRODUCT = 1;
    public static final int TARGET_POST = 2;
    public static final int TARGET_REPORT = 3;
    public static final int TARGET_WITHDRAW = 4;

    /* ========== 操作常量 ========== */
    public static final int ACTION_PENDING = 0;
    public static final int ACTION_PASS = 1;
    public static final int ACTION_REJECT = 2;
    public static final int ACTION_TAKEDOWN = 3;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PASSED = 1;
    public static final int STATUS_REJECTED = 2;
}
