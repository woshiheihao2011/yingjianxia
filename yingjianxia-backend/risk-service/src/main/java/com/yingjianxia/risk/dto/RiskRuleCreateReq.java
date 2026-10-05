package com.yingjianxia.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 风控规则创建/更新请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "风控规则创建/更新请求")
public class RiskRuleCreateReq {

    @Schema(description = "规则ID（更新时传）")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @Schema(description = "规则类型：1频率 2金额 3行为 4设备", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "规则类型不能为空")
    private Integer ruleType;

    @Schema(description = "规则配置（JSON）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则配置不能为空")
    private String ruleConfig;

    @Schema(description = "处置动作：1告警 2拦截 3验证", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "处置动作不能为空")
    private Integer action;

    @Schema(description = "状态：1启用 0停用", defaultValue = "1")
    private Integer status;
}
