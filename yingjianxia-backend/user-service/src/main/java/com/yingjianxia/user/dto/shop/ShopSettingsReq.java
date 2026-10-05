package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店铺设置更新 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "店铺设置请求")
public class ShopSettingsReq {

    @Schema(description = "客服电话")
    @Size(max = 20, message = "客服电话过长")
    private String contactPhone;

    @Schema(description = "客服微信/QQ")
    @Size(max = 50, message = "联系方式过长")
    private String contactWechat;

    @Schema(description = "营业时间", example = "周一至周日 09:00-21:00")
    @Size(max = 100, message = "营业时间过长")
    private String businessHours;

    @Schema(description = "退货地址ID（必须是当前用户的地址）")
    private Long returnAddressId;

    @Schema(description = "默认快递：顺丰/圆通/中通/韵达/EMS")
    private String defaultExpress;

    @Schema(description = "是否接受砍价")
    private Boolean acceptBargain;

    @Schema(description = "是否支持面交")
    private Boolean supportFaceTrade;

    @Schema(description = "发货时效承诺（小时）：24/48/72", example = "48")
    @Min(value = 12, message = "发货时效最少12小时")
    @Max(value = 168, message = "发货时效最多168小时")
    private Integer shipTimePromise;

    @Schema(description = "店铺公告")
    @Size(max = 500, message = "公告过长")
    private String announcement;
}
