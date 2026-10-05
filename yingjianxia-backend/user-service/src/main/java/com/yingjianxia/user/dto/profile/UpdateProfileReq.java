package com.yingjianxia.user.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "修改个人资料请求")
public class UpdateProfileReq {

    @Schema(description = "昵称(最多50字)")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Schema(description = "头像URL")
    @Size(max = 512, message = "头像URL过长")
    private String avatarUrl;

    @Schema(description = "个人简介(最多200字)")
    @Size(max = 200, message = "简介长度不能超过200个字符")
    private String bio;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Schema(description = "性别 male/female/secret")
    private String gender;

    @Schema(description = "生日 yyyy-MM-dd")
    private String birthday;

    @Schema(description = "常住地区")
    @Size(max = 100, message = "地区长度不能超过100个字符")
    private String region;
}
