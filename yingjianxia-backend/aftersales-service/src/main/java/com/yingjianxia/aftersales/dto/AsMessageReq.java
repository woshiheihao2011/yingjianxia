package com.yingjianxia.aftersales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 售后沟通消息请求
 */
@Data
@Schema(description = "售后沟通消息")
public class AsMessageReq {

    @Schema(description = "售后单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "售后单ID不能为空")
    private Long afterSaleId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "图片附件")
    private List<String> images;
}
