package com.yingjianxia.support.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帮助中心FAQ — faqs 表
 * <p>
 * 注意：faqs 主键为 INT AUTO_INCREMENT（非雪花ID），此处覆盖 BaseEntity 的 id 策略为 AUTO。
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("faqs")
public class Faq extends BaseEntity {

    /** 主键（数据库自增 INT） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类：账号/交易/物流/售后/... */
    private String category;

    /** 问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 浏览量 */
    private Integer viewCount;

    /** 有帮助数 */
    private Integer helpfulCount;

    /** 排序 */
    private Integer sortOrder;

    /** 1启用 0禁用 */
    private Integer status;

    /* ========== 本表不存在：updatedAt/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 状态常量 ========== */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}
