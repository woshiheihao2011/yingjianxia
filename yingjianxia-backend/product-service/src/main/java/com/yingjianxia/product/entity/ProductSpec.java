package com.yingjianxia.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品规格参数实体 — product_specs 表
 * <p>
 * 垂直存储：一个商品对应多行（品牌/型号/显存/功耗/插槽...）
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_specs")
public class ProductSpec extends BaseEntity {

    /** 关联商品 */
    private Long productId;

    /** 参数名（品牌/型号/接口/功耗/显存容量/核心频率...） */
    private String specName;

    /** 参数值 */
    private String specValue;

    /** 排序 */
    private Integer sortOrder;

    /* ========== deleted/version/createdAt/updatedAt 由 BaseEntity 继承，表已有对应列 ========== */
}
