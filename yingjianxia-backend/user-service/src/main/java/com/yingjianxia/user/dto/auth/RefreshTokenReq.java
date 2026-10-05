package com.yingjianxia.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 刷新 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "刷新Token请求")
public class RefreshTokenReq {

    @Schema(description = "Refresh Token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}
