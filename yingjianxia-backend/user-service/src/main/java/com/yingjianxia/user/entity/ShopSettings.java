package com.yingjianxia.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺设置实体 — shop_settings 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_settings")
public class ShopSettings extends BaseEntity {

    /**
     * 关联店铺
     */
    private Long shopId;

    /**
     * 客服电话
     */
    private String contactPhone;

    /**
     * 客服微信/QQ
     */
    private String contactWechat;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 退货地址ID
     */
    private Long returnAddressId;

    /**
     * 默认快递：顺丰/圆通/中通/韵达/EMS
     */
    private String defaultExpress;

    /**
     * 是否接受砍价
     */
    private Boolean acceptBargain;

    /**
     * 是否支持面交
     */
    private Boolean supportFaceTrade;

    /**
     * 发货时效承诺（小时）：24/48/72
     */
    private Integer shipTimePromise;

    /**
     * 店铺公告
     */
    private String announcement;

    /* ========== 覆盖 BaseEntity 中本表不存在的字段 ========== */

    @TableField(exist = false)
    private Integer deleted;

    @TableField(exist = false)
    private Integer version;
}
