package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 服务承诺 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "服务承诺响应")
public class ServicePromiseResp {

    @Schema(description = "承诺ID")
    private Long id;

    @Schema(description = "承诺编码")
    private String code;

    @Schema(description = "承诺名称")
    private String name;

    @Schema(description = "是否开启")
    private Boolean enabled;
}
