package com.yingjianxia.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.user.dto.AdminUserListReq;
import com.yingjianxia.user.dto.AdminUserResp;
import com.yingjianxia.user.entity.User;
import com.yingjianxia.user.entity.UserRole;
import com.yingjianxia.user.mapper.UserMapper;
import com.yingjianxia.user.mapper.UserRoleMapper;
import com.yingjianxia.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端用户服务实现
 *
 * @author 硬件侠后端团队
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public PageResult<AdminUserResp> listUsers(AdminUserListReq req) {
        Page<User> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();

        // 关键词搜索（昵称/手机号）
        if (StrUtil.isNotBlank(req.getKeyword())) {
            String kw = req.getKeyword();
            w.and(qw -> qw.like(User::getNickname, kw).or().like(User::getPhone, kw));
        }
        // 状态筛选
        if (req.getStatus() != null) {
            w.eq(User::getStatus, req.getStatus());
        }
        w.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(page, w);

        // 批量查询角色
        List<Long> userIds = result.getRecords().stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<String>> roleMap;
        if (!userIds.isEmpty()) {
            List<UserRole> allRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
            roleMap = allRoles.stream().collect(
                    Collectors.groupingBy(UserRole::getUserId,
                            Collectors.mapping(UserRole::getRole, Collectors.toList())));
        } else {
            roleMap = Map.of();
        }

        // 组装响应
        List<AdminUserResp> respList = result.getRecords().stream().map(u -> {
            AdminUserResp resp = new AdminUserResp();
            BeanUtils.copyProperties(u, resp);
            resp.setAvatar(u.getAvatarUrl());
            resp.setPhone(maskPhone(u.getPhone()));
            resp.setRoles(roleMap.getOrDefault(u.getId(), List.of()));
            return resp;
        }).collect(Collectors.toList());

        // 角色二次过滤（如果传了 role 参数）
        if (StrUtil.isNotBlank(req.getRole())) {
            String roleFilter = req.getRole().toUpperCase();
            respList = respList.stream()
                    .filter(r -> r.getRoles() != null && r.getRoles().stream()
                            .anyMatch(rl -> rl.equalsIgnoreCase(roleFilter)))
                    .collect(Collectors.toList());
        }

        return PageResult.of(req.getPageNum(), req.getPageSize(), result.getTotal(), respList);
    }

    /**
     * 手机号脱敏：13900001234 → 139****1234
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
