package com.yingjianxia.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 发表评价请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "发表评价请求")
public class ReviewCreateReq {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "评分 1~5 星", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小1星")
    @Max(value = 5, message = "评分最大5星")
    private Integer rating;

    @Schema(description = "评价文字", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评价内容不能为空")
    private String content;

    @Schema(description = "评价图片")
    private List<String> images;

    @Schema(description = "是否匿名")
    private Boolean isAnonymous;
}
