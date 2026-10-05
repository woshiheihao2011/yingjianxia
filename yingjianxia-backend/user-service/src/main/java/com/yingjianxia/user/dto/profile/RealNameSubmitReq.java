package com.yingjianxia.user.dto.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 实名认证提交 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "实名认证请求")
public class RealNameSubmitReq {

    @Schema(description = "真实姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "姓名长度超限")
    private String realName;

    @Schema(description = "身份证号(18位)", example = "110101199001011234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$",
            message = "身份证号格式错误")
    @JsonAlias("idCard")
    private String idCardNo;

    @Schema(description = "短信验证码（可选，不传则跳过验证码校验）")
    private String smsCode;
}
