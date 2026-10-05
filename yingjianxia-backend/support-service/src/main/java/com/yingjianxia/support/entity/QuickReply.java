package com.yingjianxia.support.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 快捷回复配置 — quick_replies 表
 * <p>
 * type：1欢迎语 2关键词回复 3快捷短语
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quick_replies")
public class QuickReply extends BaseEntity {

    /** 所属店铺 */
    private Long shopId;

    /** 1欢迎语 2关键词回复 3快捷短语 */
    private Integer type;

    /** 分组：售前/议价/售后/物流 */
    private String groupName;

    /** 触发关键词（type=2时） */
    private String keyword;

    /** 匹配方式：1精确匹配 2模糊匹配 */
    private Integer matchType;

    /** 回复内容 */
    private String replyContent;

    /** 启用状态 */
    private Integer isEnabled;

    /** 再次触发间隔（小时） */
    private Integer triggerInterval;

    /** 使用次数 */
    private Integer useCount;

    /** 排序 */
    private Integer sortOrder;

    /* ========== 本表不存在：deleted/version ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;

    /* ========== 类型常量 ========== */
    public static final int TYPE_WELCOME = 1;
    public static final int TYPE_KEYWORD = 2;
    public static final int TYPE_PHRASE = 3;
}
