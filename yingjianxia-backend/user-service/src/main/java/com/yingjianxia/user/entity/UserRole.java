package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色实体
 *
 * @author 硬件侠后端团队
 */
@Data
@TableName("user_roles")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 角色: BUYER / SELLER / AUDITOR / ADMIN */
    private String role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
