package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 实名认证实体 — user_real_name 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_real_name")
public class UserRealName extends BaseEntity {

    /**
     * 关联用户（一人一认证）
     */
    private Long userId;

    /**
     * 真实姓名（加密）
     */
    private String realName;

    /**
     * 身份证号（AES加密）
     */
    private String idCardNo;

    /**
     * 0待认证 1已通过 2未通过
     */
    private Integer verificationStatus;

    /**
     * 认证时间
     */
    private LocalDateTime verifiedAt;

    /* ========== 覆盖 BaseEntity 中本表不存在的字段 ========== */

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PASSED = 1;
    public static final int STATUS_REJECTED = 2;
}
