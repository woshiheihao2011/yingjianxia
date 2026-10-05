package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用户实体 — users 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    /**
     * 手机号（加密存储）
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * BCrypt 加密密码
     */
    private String passwordHash;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 性别 male/female/secret
     */
    private String gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 常住地区
     */
    private String region;

    /**
     * 信用分 0.0-5.0
     */
    private BigDecimal creditScore;

    /**
     * 成交笔数
     */
    private Integer transactionCount;

    /**
     * 本月收入
     */
    private BigDecimal monthlyIncome;

    /**
     * 优质卖家标签：0否 1是
     */
    @TableField("is_seller_verified")
    private Boolean sellerVerified;

    /**
     * 1正常 2冻结 3封禁
     */
    private Integer status;

    /* ========== 覆盖 BaseEntity 中本表不存在的字段 ========== */

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== 状态常量 ========== */
    /** 正常 */
    public static final int STATUS_NORMAL = 1;
    /** 冻结 */
    public static final int STATUS_FROZEN = 2;
    /** 封禁 */
    public static final int STATUS_BANNED = 3;
}
