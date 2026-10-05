package com.yingjianxia.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 风控检查请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "风控检查请求")
public class RiskCheckReq {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "动作类型（如：下单/提现/登录/发帖）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "动作类型不能为空")
    private String action;

    @Schema(description = "金额（金额类规则校验）")
    private BigDecimal amount;

    @Schema(description = "IP 地址")
    private String ipAddress;

    @Schema(description = "设备指纹ID")
    private String deviceId;
}
