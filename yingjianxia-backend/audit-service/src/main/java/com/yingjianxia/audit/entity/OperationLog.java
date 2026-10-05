package com.yingjianxia.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志 — operation_logs 表（审核域自建表）
 * <p>
 * 记录所有关键操作的审计日志，用于合规追溯。
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_logs")
public class OperationLog extends BaseEntity {

    /** 操作用户ID */
    private Long userId;

    /** 用户类型：1买家 2卖家 3审核员 4客服 5管理员 */
    private Integer userType;

    /** 操作动作 */
    private String action;

    /** 模块 */
    private String module;

    /** 目标类型 */
    private Integer targetType;

    /** 目标对象ID */
    private Long targetId;

    /** 请求数据（JSON） */
    private String requestData;

    /** IP 地址 */
    private String ipAddress;

    /* ========== 本表不存在：updatedAt/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;
}
