package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 店铺员工 创建/更新 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "店铺员工请求")
public class StaffReq {

    @Schema(description = "关联用户ID（邀请时可传）")
    private Long userId;

    @Schema(description = "角色：owner/admin/operator/customer_service", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(owner|admin|operator|customer_service)$", message = "角色不合法")
    private String role;

    @Schema(description = "权限列表（JSON 数组）")
    private List<String> permissions;

    @Schema(description = "状态：1正常 2禁用")
    private Integer status = 1;
}
