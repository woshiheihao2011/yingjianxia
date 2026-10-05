package com.yingjianxia.inspection.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.inspection.dto.InspectionSubmitReq;
import com.yingjianxia.inspection.entity.InspectionReport;
import com.yingjianxia.inspection.entity.InspectionTemplate;
import com.yingjianxia.inspection.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * 验机服务 Controller
 */
@Tag(name = "验机服务", description = "12项检测模板/验机报告/SHA-256签名验证/PDF下载")
@RestController
@RequestMapping("/api/v1/inspection")
@RequiredArgsConstructor
public class InspectionController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final InspectionService service;

    /* ========== 模板（公开） ========== */

    @Operation(summary = "查看某分类的验机模板（12项通用+分类定制）")
    @GetMapping("/templates")
    public ApiResponse<List<InspectionTemplate>> templates(
            @RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
        return ApiResponse.success(service.listTemplates(categoryId));
    }

    /* ========== 检测员端（需 ROLE_INSPECTOR） ========== */

    @Operation(summary = "[检测员] 为商品创建验机报告", hidden = false)
    @PostMapping("/report/create")
    public ApiResponse<Long> createReport(@RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Long orderId,
                                          @RequestBody(required = false) java.util.Map<String, Object> body) {
        // 兼容前端用 JSON body 传参：{"productId": 1}
        if (productId == null && body != null && body.get("productId") != null) {
            productId = Long.valueOf(body.get("productId").toString());
        }
        if (orderId == null && body != null && body.get("orderId") != null) {
            orderId = Long.valueOf(body.get("orderId").toString());
        }
        Long inspectorId = UserContext.requiredUserId();
        return ApiResponse.success(service.createReport(productId, orderId, inspectorId, "检测员" + inspectorId));
    }

    @Operation(summary = "[检测员] 开始检测（状态→检测中）")
    @PostMapping("/report/{reportId}/start")
    public ApiResponse<Void> startTesting(@PathVariable Long reportId) {
        service.startTesting(reportId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[检测员] 提交检测结果（生成签名+PDF→自动完成）")
    @PostMapping("/report/submit")
    public ApiResponse<InspectionReport> submit(@Valid @RequestBody InspectionSubmitReq req) {
        return ApiResponse.success(service.submitResult(req, UserContext.requiredUserId()));
    }

    /* ========== 查询 & 下载（公开，带签名校验） ========== */

    @Operation(summary = "验机报告详情（商品详情页/订单详情）")
    @GetMapping("/report/{reportId}")
    public ApiResponse<InspectionReport> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(service.getReportDetail(reportId));
    }

    @Operation(summary = "根据商品ID查验机报告（详情页入口）")
    @GetMapping("/report/by-product/{productId}")
    public ApiResponse<InspectionReport> getByProduct(@PathVariable Long productId) {
        return ApiResponse.success(service.getReportByProduct(productId));
    }

    @Operation(summary = "验机报告签名校验（SHA-256）— 判断是否被篡改")
    @GetMapping("/report/{reportId}/verify")
    public ApiResponse<Boolean> verify(@PathVariable Long reportId) {
        return ApiResponse.success(service.verifySignature(reportId));
    }

    @Operation(summary = "验机报告 PDF URL（OSS）")
    @GetMapping("/report/{reportId}/pdf")
    public ApiResponse<String> pdf(@PathVariable Long reportId) {
        return ApiResponse.success(service.generateOrGetPdfUrl(reportId));
    }

    /* ========== [内部] 其他服务回调 ========== */

    @Operation(summary = "[内部] 商品服务查询报告校验", hidden = true)
    @GetMapping("/internal/by-product/{productId}")
    public ApiResponse<InspectionReport> internalByProduct(
            @PathVariable Long productId,
            @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        try {
            return ApiResponse.success(service.getReportByProduct(productId));
        } catch (Exception e) {
            return ApiResponse.success(null);
        }
    }
}
