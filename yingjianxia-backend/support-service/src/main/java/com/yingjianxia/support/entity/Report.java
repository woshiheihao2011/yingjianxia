package com.yingjianxia.support.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 举报记录 — reports 表
 * <p>
 * 目标类型：1商品 2用户 3订单 4帖子
 * 原因类型：1商品违规 2虚假交易 3诈骗 4盗图 5恶意辱骂 6其他
 * 状态：0受理中 1已处理 2待处理
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reports")
public class Report extends BaseEntity {

    /** 举报人 */
    private Long reporterId;

    /** 被举报对象类型：1商品 2用户 3订单 4帖子 */
    private Integer targetType;

    /** 被举报对象ID */
    private Long targetId;

    /** 对象链接 */
    private String targetUrl;

    /** 原因类型：1商品违规 2虚假交易 3诈骗 4盗图 5恶意辱骂 6其他 */
    private Integer reasonType;

    /** 举报描述 */
    private String description;

    /** 证据图片（最多6张，JSON） */
    private String evidenceImages;

    /** 匿名举报 */
    private Integer isAnonymous;

    /** 0受理中 1已处理 2待处理 */
    private Integer status;

    /** 处理人 */
    private Long handlerId;

    /** 处理结果 */
    private String result;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /* ========== 本表不存在：updatedAt/deleted/version ========== */
    @TableField(exist = false) private java.time.LocalDateTime updatedAt;
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 目标类型常量 ========== */
    public static final int TARGET_PRODUCT = 1;
    public static final int TARGET_USER = 2;
    public static final int TARGET_ORDER = 3;
    public static final int TARGET_POST = 4;

    /* ========== 状态常量 ========== */
    public static final int STATUS_ACCEPTING = 0;
    public static final int STATUS_HANDLED = 1;
    public static final int STATUS_PENDING = 2;
}
