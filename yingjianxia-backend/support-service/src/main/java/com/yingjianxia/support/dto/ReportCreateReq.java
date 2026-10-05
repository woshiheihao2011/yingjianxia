package com.yingjianxia.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 举报请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "举报请求")
public class ReportCreateReq {

    @Schema(description = "目标类型：1商品 2用户 3订单 4帖子", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    @Schema(description = "目标对象ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标对象ID不能为空")
    private Long targetId;

    @Schema(description = "原因类型：1商品违规 2虚假交易 3诈骗 4盗图 5恶意辱骂 6其他", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原因类型不能为空")
    private Integer reasonType;

    @Schema(description = "举报描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "举报描述不能为空")
    @Size(max = 500, message = "举报描述最长500字")
    private String description;

    @Schema(description = "证据图片（最多6张）")
    private List<String> evidenceImages;

    @Schema(description = "是否匿名")
    private Boolean isAnonymous;
}
