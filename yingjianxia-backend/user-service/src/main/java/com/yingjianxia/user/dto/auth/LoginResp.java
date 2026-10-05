package com.yingjianxia.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录/注册/刷新 成功 响应 DTO — 双 Token 机制
 *
 * @author 硬件侠后端团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录成功响应（双Token）")
public class LoginResp {

    @Schema(description = "用户ID", example = "1234567890123456789")
    private Long userId;

    @Schema(description = "昵称", example = "数码爱好者")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "角色列表", example = "[\"BUYER\"]")
    private List<String> roles;

    @Schema(description = "Access Token (有效期 2h)")
    private String accessToken;

    @Schema(description = "Access Token 剩余有效毫秒数", example = "7200000")
    private Long expiresIn;

    @Schema(description = "Refresh Token (有效期 7d，用于静默刷新)")
    private String refreshToken;
}
