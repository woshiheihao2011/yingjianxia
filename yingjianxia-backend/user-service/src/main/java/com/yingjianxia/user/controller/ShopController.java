package com.yingjianxia.user.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.user.dto.shop.*;
import com.yingjianxia.user.feign.EvaluationFeignClient;
import com.yingjianxia.user.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店铺模块 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "店铺模块", description = "一键开店、店铺信息/设置管理、优质卖家认证")
@RestController
@RequestMapping("/api/v1/shop")
@RequiredArgsConstructor
public class ShopController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final ShopService shopService;
    private final EvaluationFeignClient evaluationFeignClient;

    /* ======================== 卖家端 ======================== */

    @Operation(summary = "一键开店", description = "每个用户限开一家店铺")
    @PostMapping("/open")
    public ApiResponse<Long> openShop(@Valid @RequestBody OpenShopReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.openShop(sellerId, req));
    }

    @Operation(summary = "修改店铺基础信息（名称/简介/头像/封面）")
    @PutMapping("/{shopId}")
    public ApiResponse<Void> updateShop(@PathVariable("shopId") Long shopId,
                                        @Valid @RequestBody UpdateShopReq req) {
        Long userId = UserContext.requiredUserId();
        shopService.updateShop(userId, shopId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "修改店铺设置（客服/发货/公告/退货地址等）")
    @PutMapping("/{shopId}/settings")
    public ApiResponse<Void> updateSettings(@PathVariable("shopId") Long shopId,
                                            @Valid @RequestBody ShopSettingsReq req) {
        Long userId = UserContext.requiredUserId();
        shopService.updateShopSettings(userId, shopId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "查看我的店铺（个人中心入口）", description = "未开店返回 data=null")
    @GetMapping("/mine")
    public ApiResponse<ShopResp> getMyShop() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.getShopBySeller(sellerId));
    }

    @Operation(summary = "获取我的店铺设置")
    @GetMapping("/mine/settings")
    public ApiResponse<ShopSettingsResp> getMyShopSettings() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.getShopSettings(sellerId));
    }

    /* ---------- 店铺图片上传 ---------- */

    @Operation(summary = "上传店铺图片（Logo/Banner）", description = "type 可选值：logo / banner")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadShopImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "logo") String type) {
        Long sellerId = UserContext.requiredUserId();
        String url = shopService.uploadShopImage(sellerId, file, type);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return ApiResponse.success(data);
    }

    /* ---------- 运费模板管理 ---------- */

    @Operation(summary = "获取运费模板列表")
    @GetMapping("/mine/logistics-templates")
    public ApiResponse<List<LogisticsTemplateResp>> listLogisticsTemplates() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.listLogisticsTemplates(sellerId));
    }

    @Operation(summary = "创建运费模板")
    @PostMapping("/mine/logistics-templates")
    public ApiResponse<Long> createLogisticsTemplate(@Valid @RequestBody LogisticsTemplateReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.createLogisticsTemplate(sellerId, req));
    }

    @Operation(summary = "更新运费模板")
    @PutMapping("/mine/logistics-templates/{id}")
    public ApiResponse<Void> updateLogisticsTemplate(@PathVariable("id") Long id,
                                                     @Valid @RequestBody LogisticsTemplateReq req) {
        Long sellerId = UserContext.requiredUserId();
        shopService.updateLogisticsTemplate(sellerId, id, req);
        return ApiResponse.success();
    }

    @Operation(summary = "删除运费模板")
    @DeleteMapping("/mine/logistics-templates/{id}")
    public ApiResponse<Void> deleteLogisticsTemplate(@PathVariable("id") Long id) {
        Long sellerId = UserContext.requiredUserId();
        shopService.deleteLogisticsTemplate(sellerId, id);
        return ApiResponse.success();
    }

    /* ---------- 服务承诺管理 ---------- */

    @Operation(summary = "获取服务承诺列表")
    @GetMapping("/mine/service-promises")
    public ApiResponse<List<ServicePromiseResp>> listServicePromises() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.listServicePromises(sellerId));
    }

    @Operation(summary = "更新服务承诺开关")
    @PutMapping("/mine/service-promises/{id}")
    public ApiResponse<Void> updateServicePromise(@PathVariable("id") Long id,
                                                  @RequestBody Map<String, Boolean> body) {
        Long sellerId = UserContext.requiredUserId();
        shopService.updateServicePromise(sellerId, id, body.get("enabled"));
        return ApiResponse.success();
    }

    /* ---------- 员工管理 ---------- */

    @Operation(summary = "获取员工列表")
    @GetMapping("/mine/staff")
    public ApiResponse<List<StaffResp>> listStaff() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.listStaff(sellerId));
    }

    @Operation(summary = "添加员工")
    @PostMapping("/mine/staff")
    public ApiResponse<Long> addStaff(@Valid @RequestBody StaffReq req) {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.addStaff(sellerId, req));
    }

    @Operation(summary = "更新员工权限")
    @PutMapping("/mine/staff/{id}")
    public ApiResponse<Void> updateStaff(@PathVariable("id") Long id,
                                         @Valid @RequestBody StaffReq req) {
        Long sellerId = UserContext.requiredUserId();
        shopService.updateStaff(sellerId, id, req);
        return ApiResponse.success();
    }

    @Operation(summary = "移除员工")
    @DeleteMapping("/mine/staff/{id}")
    public ApiResponse<Void> removeStaff(@PathVariable("id") Long id) {
        Long sellerId = UserContext.requiredUserId();
        shopService.removeStaff(sellerId, id);
        return ApiResponse.success();
    }

    /* ---------- 认证资质 ---------- */

    @Operation(summary = "获取店铺认证状态列表")
    @GetMapping("/mine/certifications")
    public ApiResponse<List<CertificationResp>> listCertifications() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.listCertifications(sellerId));
    }

    /* ---------- 财务设置 ---------- */

    @Operation(summary = "获取财务设置")
    @GetMapping("/mine/finance")
    public ApiResponse<FinanceResp> getFinance() {
        Long sellerId = UserContext.requiredUserId();
        return ApiResponse.success(shopService.getFinance(sellerId));
    }

    @Operation(summary = "更新财务设置")
    @PutMapping("/mine/finance")
    public ApiResponse<Void> updateFinance(@Valid @RequestBody FinanceReq req) {
        Long sellerId = UserContext.requiredUserId();
        shopService.updateFinance(sellerId, req);
        return ApiResponse.success();
    }

    @Operation(summary = "申请优质卖家认证",
            description = "前置：已实名认证 + 已开店 + 成交笔数≥10 + 信用分≥4.5")
    @PostMapping("/seller-verification/apply")
    public ApiResponse<Void> applySellerVerification() {
        Long userId = UserContext.requiredUserId();
        shopService.applySellerVerification(userId);
        return ApiResponse.success();
    }

    /* ======================== 买家端（公开） ======================== */

    @Operation(summary = "按店铺ID查店铺详情（买家浏览用）")
    @GetMapping("/{shopId}")
    public ApiResponse<ShopResp> getShop(@PathVariable("shopId") Long shopId) {
        return ApiResponse.success(shopService.getShop(shopId));
    }

    @Operation(summary = "按卖家用户ID查店铺（从个人主页跳转）")
    @GetMapping("/by-seller/{sellerId}")
    public ApiResponse<ShopResp> getShopBySeller(@PathVariable("sellerId") Long sellerId) {
        return ApiResponse.success(shopService.getShopBySeller(sellerId));
    }

    @Operation(summary = "店铺统计（评分分布、好评率、评价总数）")
    @GetMapping({"/{shopId}/reviews/stats", "/{shopId}/stats"})
    public ApiResponse<Map<String, Object>> reviewStats(@PathVariable("shopId") Long shopId) {
        // 通过 shopId 查店铺获取 sellerId，再调 evaluation-service 获取真实评价统计
        ShopResp shop = shopService.getShop(shopId);
        if (shop == null || shop.getSellerId() == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("shopId", shopId);
            empty.put("totalCount", 0);
            empty.put("averageRating", 0.0);
            empty.put("positiveRate", 0.0);
            Map<String, Integer> dist = new HashMap<>();
            for (int i = 1; i <= 5; i++) dist.put(String.valueOf(i), 0);
            empty.put("ratingDistribution", dist);
            return ApiResponse.success(empty);
        }
        // 调用 evaluation-service 获取真实统计
        try {
            var evalResp = evaluationFeignClient.sellerReviewStats(shop.getSellerId());
            if (evalResp != null && evalResp.isSuccess() && evalResp.getData() != null) {
                Map<String, Object> stats = new HashMap<>(evalResp.getData());
                stats.put("shopId", shopId);
                return ApiResponse.success(stats);
            }
        } catch (Exception e) {
            // evaluation-service 不可用时返回空统计，不阻断店铺页
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("shopId", shopId);
        fallback.put("totalCount", 0);
        fallback.put("averageRating", 0.0);
        fallback.put("positiveRate", 0.0);
        Map<String, Integer> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) dist.put(String.valueOf(i), 0);
        fallback.put("ratingDistribution", dist);
        return ApiResponse.success(fallback);
    }

    /* ======================== [内部] MQ/其他服务调用 ======================== */

    @Operation(summary = "[内部] 刷新店铺冗余指标（订单完成/商品上下架后调用）", hidden = true)
    @PostMapping("/internal/{shopId}/refresh")
    public ApiResponse<Void> refreshMetrics(@PathVariable("shopId") Long shopId,
                                            @RequestParam("sellerId") Long sellerId,
                                            @RequestHeader(value = "X-Internal-Secret", defaultValue = "") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.success();
        shopService.refreshShopMetrics(shopId, sellerId);
        return ApiResponse.success();
    }
}
