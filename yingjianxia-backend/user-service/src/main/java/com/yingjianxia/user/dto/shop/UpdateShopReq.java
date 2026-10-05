package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店铺基础信息修改 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "修改店铺信息请求")
public class UpdateShopReq {

    @Schema(description = "店铺名称")
    @Size(min = 3, max = 100, message = "店铺名称长度需在3-100个字符之间")
    private String shopName;

    @Schema(description = "店铺简介")
    @Size(max = 1000, message = "简介过长")
    private String description;

    @Schema(description = "店铺头像URL")
    @Size(max = 512, message = "头像URL过长")
    private String logoUrl;

    @Schema(description = "封面图URL")
    @Size(max = 512, message = "封面URL过长")
    private String coverUrl;
}
