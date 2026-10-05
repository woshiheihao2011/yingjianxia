package com.yingjianxia.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录设备信息 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "登录设备信息")
public class LoginDeviceResp {

    @Schema(description = "Token JTI（踢出设备时使用）")
    private String jti;

    @Schema(description = "设备名称", example = "Chrome on Windows")
    private String deviceName;

    @Schema(description = "操作系统", example = "Windows")
    private String os;

    @Schema(description = "浏览器", example = "Chrome")
    private String browser;

    @Schema(description = "登录IP")
    private String ip;

    @Schema(description = "登录时间")
    private LocalDateTime loginAt;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveAt;

    @Schema(description = "是否当前设备")
    private Boolean current;
}
