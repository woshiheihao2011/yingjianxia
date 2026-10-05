package com.yingjianxia.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方 OAuth2 登录 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "第三方登录请求")
public class OAuthLoginReq {

    @Schema(description = "第三方类型: weixin/qq/alipay", example = "weixin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "第三方类型不能为空")
    private String provider;

    @Schema(description = "授权码code", example = "code_xxx_from_wechat", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权码不能为空")
    private String code;

    @Schema(description = "微信小程序/公众号专用: 未绑定用户时返回的临时凭证(首次登录前端再传手机号用)", hidden = true)
    private String state;
}
