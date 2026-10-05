package com.yingjianxia.user.service;

import com.yingjianxia.user.dto.profile.*;

import java.util.List;

/**
 * 用户资料服务接口 — 个人中心/信用分/实名认证/收货地址
 *
 * @author 硬件侠后端团队
 */
public interface UserProfileService {

    /**
     * 获取当前登录用户的个人中心资料
     *
     * @param userId 用户ID
     * @return 资料视图
     */
    UserProfileResp getProfile(Long userId);

    /**
     * 修改个人资料（昵称/头像/简介）
     *
     * @param userId 用户ID
     * @param req    修改请求
     */
    void updateProfile(Long userId, UpdateProfileReq req);

    /**
     * 修改密码（验证旧密码后更新）
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 提交实名认证申请
     *
     * @param userId 用户ID
     * @param req    实名信息 + 验证码
     */
    void submitRealName(Long userId, RealNameSubmitReq req);

    /**
     * 查询用户实名状态
     *
     * @param userId 用户ID
     * @return 0待认证 1已通过 2未通过
     */
    Integer getRealNameStatus(Long userId);

    /* =================== 收货地址 =================== */

    /**
     * 新增收货地址
     *
     * @param userId 用户ID
     * @param req    地址内容
     * @return 地址ID
     */
    Long addAddress(Long userId, AddressReq req);

    /**
     * 修改收货地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @param req       地址内容
     */
    void updateAddress(Long userId, Long addressId, AddressReq req);

    /**
     * 删除收货地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void setDefaultAddress(Long userId, Long addressId);

    /**
     * 查询用户的收货地址列表（默认地址排在最前）
     *
     * @param userId 用户ID
     * @return 列表
     */
    List<AddressResp> listAddress(Long userId);

    /**
     * 查询单个收货地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @return 地址
     */
    AddressResp getAddress(Long userId, Long addressId);

    /* =================== 信用分 =================== */

    /**
     * 查询用户信用分
     *
     * @param userId 用户ID
     * @return 信用分（0.0-5.0）
     */
    java.math.BigDecimal getCreditScore(Long userId);

    /**
     * 批量刷新用户信用分 — XXL-Job 调度（每日凌晨）
     * 算法：基础分 + 成交率 × 系数 - 差评扣分 - 纠纷扣分 - 违约扣分
     */
    void refreshCreditScoreBatch();
}
