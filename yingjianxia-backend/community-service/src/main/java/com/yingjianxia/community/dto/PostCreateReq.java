package com.yingjianxia.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发帖请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "发帖请求")
public class PostCreateReq {

    @Schema(description = "分类：1装机指南 2避坑攻略 3硬件评测 4二手验机 5问答互助 6晒单", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分类不能为空")
    private Integer category;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100字")
    private String title;

    @Schema(description = "正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "正文不能为空")
    private String content;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "图片列表")
    private List<String> images;
}
