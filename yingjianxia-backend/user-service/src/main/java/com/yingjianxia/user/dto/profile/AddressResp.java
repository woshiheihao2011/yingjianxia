package com.yingjianxia.user.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "收货地址响应")
public class AddressResp {

    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "收件人")
    private String receiverName;

    @Schema(description = "联系电话(脱敏)")
    private String phone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String detail;

    @Schema(description = "拼接的完整地址")
    private String fullAddress;

    @Schema(description = "是否默认地址")
    private Boolean isDefault;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
