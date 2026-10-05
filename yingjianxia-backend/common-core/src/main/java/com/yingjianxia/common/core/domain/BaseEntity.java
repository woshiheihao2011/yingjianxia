package com.yingjianxia.common.core.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类 — 所有持久化 Entity 必须继承
 * <p>
 * 强制包含以下通用字段：
 * <ul>
 *   <li>id           : 主键 BIGINT，雪花算法生成</li>
 *   <li>created_at   : 创建时间（自动填充）</li>
 *   <li>updated_at   : 更新时间（自动填充）</li>
 *   <li>deleted      : 逻辑删除 0=未删 1=已删</li>
 *   <li>version      : 乐观锁版本号</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间（插入时自动填充）
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间（插入和更新时自动填充）
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0 正常，1 已删除
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted;

    /**
     * 乐观锁版本号（资金类/库存类表必须启用，普通表可 @TableField(exist=false) 忽略）
     */
    @Version
    @TableField(value = "version")
    private Integer version;
}
