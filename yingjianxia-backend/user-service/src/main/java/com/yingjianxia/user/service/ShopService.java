package com.yingjianxia.user.service;

import com.yingjianxia.user.dto.shop.*;

import java.util.List;

/**
 * 店铺服务接口 — 一键开店/店铺设置/优质卖家认证/运营子模块
 *
 * @author 硬件侠后端团队
 */
public interface ShopService {

    /**
     * 一键开店
     *
     * @param sellerId 卖家用户ID
     * @param req      店铺名称/简介/头像
     * @return 店铺ID
     */
    Long openShop(Long sellerId, OpenShopReq req);

    /**
     * 修改店铺基础信息（名称/简介/头像/封面）
     *
     * @param userId  当前登录用户
     * @param shopId  店铺ID
     * @param req     修改内容
     */
    void updateShop(Long userId, Long shopId, UpdateShopReq req);

    /**
     * 修改店铺设置（发货/售后/联系方式/公告等）
     *
     * @param userId  当前登录用户
     * @param shopId  店铺ID
     * @param req     设置内容
     */
    void updateShopSettings(Long userId, Long shopId, ShopSettingsReq req);

    /**
     * 按店铺ID查店铺详情（含设置冗余、卖家信用分）
     *
     * @param shopId 店铺ID
     * @return 详情
     */
    ShopResp getShop(Long shopId);

    /**
     * 按卖家用户ID查店铺（个人中心"我的店铺"入口）
     *
     * @param sellerId 卖家ID
     * @return 店铺详情（未开店返回 null）
     */
    ShopResp getShopBySeller(Long sellerId);

    /**
     * 申请优质卖家认证
     * <p>
     * 前置条件：已实名认证、已开店、成交笔数≥10、信用分≥4.5
     *
     * @param userId 申请用户
     */
    void applySellerVerification(Long userId);

    /**
     * 事件驱动 — 订单完成后刷新店铺冗余字段（成交量/在售数/评分）
     * 供消息监听器调用
     *
     * @param shopId   店铺ID
     * @param sellerId 卖家ID
     */
    void refreshShopMetrics(Long shopId, Long sellerId);

    /* ============================================================
     *  店铺设置查询
     * ============================================================ */

    /**
     * 获取当前卖家的店铺设置
     *
     * @param sellerId 卖家ID
     * @return 店铺设置（不存在则创建默认并返回）
     */
    ShopSettingsResp getShopSettings(Long sellerId);

    /* ============================================================
     *  运费模板管理
     * ============================================================ */

    List<LogisticsTemplateResp> listLogisticsTemplates(Long sellerId);

    Long createLogisticsTemplate(Long sellerId, LogisticsTemplateReq req);

    void updateLogisticsTemplate(Long sellerId, Long templateId, LogisticsTemplateReq req);

    void deleteLogisticsTemplate(Long sellerId, Long templateId);

    /* ============================================================
     *  服务承诺管理
     * ============================================================ */

    List<ServicePromiseResp> listServicePromises(Long sellerId);

    void updateServicePromise(Long sellerId, Long promiseId, Boolean enabled);

    /* ============================================================
     *  员工管理
     * ============================================================ */

    List<StaffResp> listStaff(Long sellerId);

    Long addStaff(Long sellerId, StaffReq req);

    void updateStaff(Long sellerId, Long staffId, StaffReq req);

    void removeStaff(Long sellerId, Long staffId);

    /* ============================================================
     *  认证资质
     * ============================================================ */

    List<CertificationResp> listCertifications(Long sellerId);

    /* ============================================================
     *  财务设置
     * ============================================================ */

    FinanceResp getFinance(Long sellerId);

    void updateFinance(Long sellerId, FinanceReq req);

    /* ============================================================
     *  店铺图片上传
     * ============================================================ */

    /**
     * 上传店铺图片（Logo/Banner）
     *
     * @param sellerId 卖家ID
     * @param file     图片文件
     * @param type     类型：logo / banner
     * @return 可访问的图片 URL
     */
    String uploadShopImage(Long sellerId, org.springframework.web.multipart.MultipartFile file, String type);
}
