package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 店铺认证资质实体 — shop_certifications 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_certifications")
public class ShopCertification extends BaseEntity {

    /** 关联店铺 */
    private Long shopId;

    /** 认证类型：business_license / deposit / category_access / real_name */
    private String type;

    /** 认证名称 */
    private String name;

    /** 状态：not_applied / pending / approved / rejected */
    private String status;

    /** 认证通过时间 */
    private LocalDateTime certifiedAt;

    /** 到期时间 */
    private LocalDateTime expireAt;
}
