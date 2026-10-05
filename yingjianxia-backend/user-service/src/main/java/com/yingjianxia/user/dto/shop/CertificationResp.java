package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺认证资质 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "店铺认证资质响应")
public class CertificationResp {

    @Schema(description = "认证ID")
    private Long id;

    @Schema(description = "认证类型：business_license/deposit/category_access/real_name")
    private String type;

    @Schema(description = "认证名称")
    private String name;

    @Schema(description = "状态：not_applied/pending/approved/rejected")
    private String status;

    @Schema(description = "认证通过时间")
    private LocalDateTime certifiedAt;

    @Schema(description = "到期时间")
    private LocalDateTime expireAt;
}
