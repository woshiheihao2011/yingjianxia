package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 第三方登录绑定实体 — user_auth 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_auth")
public class UserAuth extends BaseEntity {

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 第三方：weixin / qq / alipay
     */
    private String provider;

    /**
     * 第三方唯一ID
     */
    private String openId;

    /**
     * 微信 UnionID
     */
    private String unionId;

    /* ========== 覆盖 BaseEntity 中本表不存在的字段 ========== */

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;

    /* ========== Provider 常量 ========== */
    public static final String PROVIDER_WEIXIN = "weixin";
    public static final String PROVIDER_QQ = "qq";
    public static final String PROVIDER_ALIPAY = "alipay";
}
