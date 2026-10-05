package com.yingjianxia.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 每日签到响应
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "每日签到响应")
public class SigninResp {

    @Schema(description = "签到日期")
    private LocalDate signinDate;

    @Schema(description = "本次获得积分")
    private Integer pointsEarned;

    @Schema(description = "连续签到天数")
    private Integer continuousDays;

    @Schema(description = "当前可用积分")
    private Integer availablePoints;
}
