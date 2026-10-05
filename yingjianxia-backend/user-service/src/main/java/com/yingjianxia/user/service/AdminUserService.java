package com.yingjianxia.user.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.user.dto.AdminUserListReq;
import com.yingjianxia.user.dto.AdminUserResp;

/**
 * 管理端用户服务接口
 *
 * @author 硬件侠后端团队
 */
public interface AdminUserService {

    /**
     * 管理端用户列表
     */
    PageResult<AdminUserResp> listUsers(AdminUserListReq req);
}
