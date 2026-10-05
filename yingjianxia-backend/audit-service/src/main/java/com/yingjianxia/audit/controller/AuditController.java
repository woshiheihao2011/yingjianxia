package com.yingjianxia.audit.controller;

import com.yingjianxia.audit.dto.AuditQueryReq;
import com.yingjianxia.audit.dto.OperationLogReq;
import com.yingjianxia.audit.dto.ProcessAuditReq;
import com.yingjianxia.audit.entity.AuditRecord;
import com.yingjianxia.audit.entity.OperationLog;
import com.yingjianxia.audit.service.AuditService;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审核服务 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "审核服务", description = "提交审核/审核处理/审核记录查询/操作日志查询")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService service;

    /* ========== 审核流程 ========== */

    @Operation(summary = "提交审核（商品/帖子/举报/提现）")
    @PostMapping("/submit")
    public ApiResponse<Long> submitAudit(@RequestParam(required = false) Integer targetType,
                                         @RequestParam(required = false) Long targetId,
                                         @RequestBody(required = false) java.util.Map<String, Object> body) {
        // 兼容前端用 JSON body 传参：{"productId": 1} → targetType=1(商品), targetId=productId
        if (targetType == null && body != null) {
            if (body.get("targetType") != null) {
                targetType = Integer.valueOf(body.get("targetType").toString());
            } else if (body.get("productId") != null) {
                targetType = 1; // 商品审核
            }
        }
        if (targetId == null && body != null) {
            if (body.get("targetId") != null) {
                targetId = Long.valueOf(body.get("targetId").toString());
            } else if (body.get("productId") != null) {
                targetId = Long.valueOf(body.get("productId").toString());
            }
        }
        return ApiResponse.success(service.submitAudit(targetType, targetId));
    }

    @Operation(summary = "[审核员] 审核处理")
    @PostMapping("/records/{recordId}/process")
    public ApiResponse<AuditRecord> processAudit(@PathVariable Long recordId,
                                                 @Valid @RequestBody ProcessAuditReq req) {
        Long auditorId = UserContext.requiredUserId();
        return ApiResponse.success(service.processAudit(recordId, req, auditorId, "审核员" + auditorId));
    }

    @Operation(summary = "审核记录查询")
    @GetMapping("/records")
    public ApiResponse<PageResult<AuditRecord>> listRecords(AuditQueryReq req) {
        return ApiResponse.success(service.listAuditRecords(req));
    }

    @Operation(summary = "待审核记录列表")
    @GetMapping("/pending")
    public ApiResponse<PageResult<AuditRecord>> pendingRecords(AuditQueryReq req) {
        req.setStatus(AuditRecord.STATUS_PENDING);
        return ApiResponse.success(service.listAuditRecords(req));
    }

    @Operation(summary = "审核记录详情")
    @GetMapping("/records/{recordId}")
    public ApiResponse<AuditRecord> getRecord(@PathVariable Long recordId) {
        return ApiResponse.success(service.getAuditRecord(recordId));
    }

    /* ========== 操作日志 ========== */

    @Operation(summary = "[内部] 记录操作日志", hidden = true)
    @PostMapping("/internal/logs")
    public ApiResponse<Void> recordLog(@RequestBody OperationLog log) {
        service.recordOperationLog(log);
        return ApiResponse.success();
    }

    @Operation(summary = "[管理端] 操作日志查询")
    @GetMapping("/logs")
    public ApiResponse<PageResult<OperationLog>> listLogs(OperationLogReq req) {
        return ApiResponse.success(service.listOperationLogs(req));
    }
}
