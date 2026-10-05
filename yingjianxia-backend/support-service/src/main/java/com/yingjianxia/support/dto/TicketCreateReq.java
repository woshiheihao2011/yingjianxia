package com.yingjianxia.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交工单请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "提交工单请求")
public class TicketCreateReq {

    @Schema(description = "问题类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题类型不能为空")
    private String problemType;

    @Schema(description = "优先级：1高 2中 3低", defaultValue = "2")
    private Integer priority;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100字")
    private String title;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题描述不能为空")
    private String description;

    @Schema(description = "图片附件")
    private List<String> images;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "关联订单ID")
    private Long orderId;
}
