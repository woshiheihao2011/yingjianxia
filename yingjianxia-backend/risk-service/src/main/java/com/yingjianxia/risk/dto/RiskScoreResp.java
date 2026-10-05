package com.yingjianxia.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户风险评分响应
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "用户风险评分")
public class RiskScoreResp {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "风险评分（0~100，越高风险越大）")
    private Integer score;

    @Schema(description = "风险等级：1低 2中 3高")
    private Integer level;

    @Schema(description = "风险等级描述")
    private String levelDesc;
}
