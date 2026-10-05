package com.yingjianxia.user.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端用户列表查询请求
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户列表查询")
public class AdminUserListReq extends PageQuery {

    @Schema(description = "搜索关键词（昵称/手机号）")
    private String keyword;

    @Schema(description = "角色筛选（BUYER/SELLER/AUDITOR/ADMIN）")
    private String role;

    @Schema(description = "状态筛选（1=正常 2=冻结 3=封禁）")
    private Integer status;
}
