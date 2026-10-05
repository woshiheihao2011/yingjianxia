package com.yingjianxia.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 审核处理请求（审核员处理时提交）
 * targetType 和 targetId 从审核记录中获取，不需要前端传
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "审核处理请求")
public class ProcessAuditReq {

    @Schema(description = "审核操作：1通过 2拒绝 3下架", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核操作不能为空")
    private Integer action;

    @Schema(description = "审核理由")
    private String reason;

    @Schema(description = "审核意见")
    private String comment;

    @Schema(description = "证据附件")
    private List<String> evidence;
}
