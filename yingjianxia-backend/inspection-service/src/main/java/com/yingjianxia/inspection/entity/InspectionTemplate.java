package com.yingjianxia.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 验机模板配置 — inspection_templates 表（按分类定制12项）
 */
@Data
@TableName("inspection_templates")
public class InspectionTemplate implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 适用分类；0=通用 */
    private Integer categoryId;

    /** 检测项序号 1-12 */
    private Integer itemNo;

    /** 检测项名称 */
    private String itemName;

    /** 说明 */
    private String itemDesc;

    /** 通过标准描述 */
    private String passCriteria;

    /** 排序 */
    private Integer sortOrder;

    /** 1启用 0停用 */
    private Integer status;
}
