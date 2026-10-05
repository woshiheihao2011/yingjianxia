package com.yingjianxia.risk.controller;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.risk.dto.RiskCheckReq;
import com.yingjianxia.risk.dto.RiskRuleCreateReq;
import com.yingjianxia.risk.dto.RiskScoreResp;
import com.yingjianxia.risk.entity.RiskRecord;
import com.yingjianxia.risk.entity.RiskRule;
import com.yingjianxia.risk.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 风控服务 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "风控服务", description = "风控检查/记录风控事件；管理端：规则CRUD/风险记录查询/用户风险评分查询")
@RestController
@RequestMapping({"/api/v1/risk", "/api/v1/trust"})
@RequiredArgsConstructor
public class RiskController {

    private final RiskService service;

    /* ========== 内部：风控检查 ========== */

    @Operation(summary = "[内部] 风控检查", hidden = true)
    @PostMapping("/internal/check")
    public ApiResponse<RiskService.RiskCheckResult> checkRisk(@Valid @RequestBody RiskCheckReq req) {
        return ApiResponse.success(service.checkRisk(req));
    }

    @Operation(summary = "[内部] 记录风控事件", hidden = true)
    @PostMapping("/internal/records")
    public ApiResponse<Long> recordRiskEvent(@RequestParam Long userId,
                                              @RequestParam String riskType,
                                              @RequestParam Integer riskLevel,
                                              @RequestParam(required = false) Long ruleId,
                                              @RequestParam String description,
                                              @RequestParam Integer action,
                                              @RequestParam(required = false) String ipAddress,
                                              @RequestParam(required = false) String deviceId) {
        return ApiResponse.success(service.recordRiskEvent(userId, riskType, riskLevel, ruleId,
                description, action, ipAddress, deviceId));
    }

    /* ========== 管理端：规则管理 ========== */

    @Operation(summary = "[管理端] 规则列表")
    @GetMapping("/rules")
    public ApiResponse<PageResult<RiskRule>> listRules(
            @RequestParam(value = "ruleType", required = false) Integer ruleType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.listRules(ruleType, status, pageNum, pageSize));
    }

    @Operation(summary = "[管理端] 新增/更新规则")
    @PostMapping("/rules")
    public ApiResponse<Long> saveRule(@Valid @RequestBody RiskRuleCreateReq req) {
        return ApiResponse.success(service.saveRule(req));
    }

    @Operation(summary = "[管理端] 删除规则")
    @DeleteMapping("/rules/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable Long ruleId) {
        service.deleteRule(ruleId);
        return ApiResponse.success();
    }

    @Operation(summary = "[管理端] 启用/停用规则")
    @PostMapping("/rules/{ruleId}/toggle")
    public ApiResponse<Void> toggleRule(@PathVariable Long ruleId, @RequestParam Integer status) {
        service.toggleRuleStatus(ruleId, status);
        return ApiResponse.success();
    }

    /* ========== 管理端：风险记录与评分 ========== */

    @Operation(summary = "[管理端] 风险记录查询")
    @GetMapping("/records")
    public ApiResponse<PageResult<RiskRecord>> listRecords(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "riskLevel", required = false) Integer riskLevel,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.listRiskRecords(userId, riskLevel, pageNum, pageSize));
    }

    @Operation(summary = "[管理端] 用户风险评分查询")
    @GetMapping("/scores/{userId}")
    public ApiResponse<RiskScoreResp> getScore(@PathVariable Long userId) {
        return ApiResponse.success(service.getUserRiskScore(userId));
    }

    @Operation(summary = "信任概览（前端 /trust/overview）")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> trustOverview() {
        // TODO: 接入真实信用评分、风控记录统计
        Map<String, Object> overview = new HashMap<>();
        overview.put("creditScore", 100);
        overview.put("trustLevel", "EXCELLENT");
        overview.put("riskLevel", "LOW");
        overview.put("riskRecordCount", 0);
        overview.put("positiveCount", 0);
        overview.put("negativeCount", 0);
        return ApiResponse.success(overview);
    }
}
