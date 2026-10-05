package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 店铺设置 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "店铺设置响应")
public class ShopSettingsResp {

    @Schema(description = "客服电话")
    private String contactPhone;

    @Schema(description = "客服微信/QQ")
    private String contactWechat;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "退货地址ID")
    private Long returnAddressId;

    @Schema(description = "默认快递")
    private String defaultExpress;

    @Schema(description = "是否接受砍价")
    private Boolean acceptBargain;

    @Schema(description = "是否支持面交")
    private Boolean supportFaceTrade;

    @Schema(description = "发货时效承诺（小时）")
    private Integer shipTimePromise;

    @Schema(description = "店铺公告")
    private String announcement;

    @Schema(description = "默认运费模板ID")
    private Long logisticsId;

    @Schema(description = "服务承诺列表")
    private List<ServicePromiseResp> servicePromises;
}
