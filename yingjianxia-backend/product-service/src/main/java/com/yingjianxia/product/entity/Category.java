package com.yingjianxia.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体 — categories 表
 * <p>
 * 注：分类为运营配置（自增 INT 主键），不继承 BaseEntity（无 deleted/version/雪花ID）。
 *
 * @author 硬件侠后端团队
 */
@Data
@TableName("categories")
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 分类名称 */
    private String name;

    /** 分类编码：gpu/cpu/motherboard/memory/storage/cooler/psu/case */
    private String code;

    /** 分类图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /* ========== 常量 ========== */
    public static final int STATUS_ENABLE = 1;
    public static final int STATUS_DISABLE = 0;
}
