package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺员工/子账号实体 — shop_staff 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_staff")
public class ShopStaff extends BaseEntity {

    /** 关联店铺 */
    private Long shopId;

    /** 关联用户ID（null 表示待邀请/未注册） */
    private Long userId;

    /** 角色：owner / admin / operator / customer_service */
    private String role;

    /** 权限列表（JSON 数组字符串） */
    private String permissions;

    /** 状态：1正常 2禁用 */
    private Integer status;
}
