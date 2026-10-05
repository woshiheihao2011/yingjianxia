package com.yingjianxia.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 积分查询响应
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "积分查询响应")
public class PointsQueryResp {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "总积分")
    private Integer totalPoints;

    @Schema(description = "可用积分")
    private Integer availablePoints;

    @Schema(description = "已使用积分")
    private Integer usedPoints;

    @Schema(description = "已过期积分")
    private Integer expiredPoints;
}
