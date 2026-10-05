package com.yingjianxia.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 审核请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "审核请求")
public class AuditReq {

    @Schema(description = "目标类型：1商品 2帖子 3举报 4提现", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    @Schema(description = "目标对象ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标对象ID不能为空")
    private Long targetId;

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
