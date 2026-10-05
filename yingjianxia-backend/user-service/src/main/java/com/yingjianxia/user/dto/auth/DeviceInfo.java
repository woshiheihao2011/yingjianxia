package com.yingjianxia.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录设备信息（从 HTTP 请求头提取）
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "登录设备信息")
public class DeviceInfo {

    @Schema(description = "User-Agent 原始值")
    private String userAgent;

    @Schema(description = "客户端IP")
    private String ip;
}
