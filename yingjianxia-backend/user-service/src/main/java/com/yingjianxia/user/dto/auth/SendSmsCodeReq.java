package com.yingjianxia.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "发送短信验证码请求")
public class SendSmsCodeReq {

    @Schema(description = "手机号", example = "13812345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Schema(description = "场景：register注册/login登录/resetPwd找回密码/realName实名认证",
            example = "register", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "场景不能为空")
    private String scene;
}
