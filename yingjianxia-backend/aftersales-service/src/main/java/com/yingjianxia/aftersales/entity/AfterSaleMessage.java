package com.yingjianxia.aftersales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 售后沟通记录 — after_sale_messages 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale_messages")
public class AfterSaleMessage extends BaseEntity {

    /** 关联售后单 */
    private Long afterSaleId;

    /** 发送者ID */
    private Long senderId;

    /** 1买家 2卖家 3客服 */
    private Integer senderType;

    /** 消息内容 */
    private String content;

    /** 图片附件（JSON） */
    private String images;

    /* ========== 本表不存在：deleted/version/updated_at ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;
    @TableField(exist = false) private LocalDateTime updatedAt;
}
