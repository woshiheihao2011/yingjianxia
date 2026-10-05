package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 一键开店 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "开通店铺请求")
public class OpenShopReq {

    @Schema(description = "店铺名称(3-100字)", requiredMode = Schema.RequiredMode.REQUIRED, example = "小王二手数码店")
    @NotBlank(message = "店铺名称不能为空")
    @Size(min = 3, max = 100, message = "店铺名称长度需在3-100个字符之间")
    private String shopName;

    @Schema(description = "店铺简介", example = "专注精品二手数码，每台设备验机把关")
    @Size(max = 1000, message = "店铺简介过长")
    private String description;

    @Schema(description = "店铺头像URL")
    @Size(max = 512, message = "头像URL过长")
    private String logoUrl;
}
