package com.yingjianxia.user.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.user.dto.AdminUserListReq;
import com.yingjianxia.user.dto.AdminUserResp;
import com.yingjianxia.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 管理端 Controller — 用户管理
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "管理端-用户管理", description = "用户列表、状态管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

    /**
     * 管理端用户列表（需要 AUDITOR/ADMIN 角色）
     */
    @Operation(summary = "管理端用户列表")
    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserResp>> listUsers(AdminUserListReq req) {
        // 业务层角色校验
        String roles = UserContext.get().getRoles();
        if (roles == null || roles.isBlank()) {
            throw new BusinessException(403, "无权限访问管理端接口");
        }
        List<String> roleList = Arrays.asList(roles.split(","));
        boolean hasAdminRole = roleList.stream()
                .anyMatch(r -> "AUDITOR".equalsIgnoreCase(r.trim()) || "ADMIN".equalsIgnoreCase(r.trim()));
        if (!hasAdminRole) {
            throw new BusinessException(403, "无权限访问管理端接口");
        }
        return ApiResponse.success(adminUserService.listUsers(req));
    }
}
