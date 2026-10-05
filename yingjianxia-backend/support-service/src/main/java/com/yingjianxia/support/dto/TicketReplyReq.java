package com.yingjianxia.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 回复工单请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "回复工单请求")
public class TicketReplyReq {

    @Schema(description = "工单ID（路径参数时可空，由Controller注入）")
    private Long ticketId;

    @Schema(description = "回复内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "回复内容不能为空")
    private String content;

    @Schema(description = "补充附件")
    private List<String> images;
}
