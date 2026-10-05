package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 店铺员工 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "店铺员工响应")
public class StaffResp {

    @Schema(description = "员工记录ID")
    private Long id;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "用户昵称（关联用户时返回）")
    private String nickname;

    @Schema(description = "角色：owner/admin/operator/customer_service")
    private String role;

    @Schema(description = "权限列表")
    private List<String> permissions;

    @Schema(description = "状态：1正常 2禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
