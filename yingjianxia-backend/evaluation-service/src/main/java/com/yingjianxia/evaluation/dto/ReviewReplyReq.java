package com.yingjianxia.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 卖家回复评价请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "卖家回复评价请求")
public class ReviewReplyReq {

    @Schema(description = "评价ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评价ID不能为空")
    private Long reviewId;

    @Schema(description = "回复内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容最长500字")
    private String replyContent;
}
