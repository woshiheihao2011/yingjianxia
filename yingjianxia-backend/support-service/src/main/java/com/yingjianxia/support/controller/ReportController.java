package com.yingjianxia.support.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.support.dto.ReportCreateReq;
import com.yingjianxia.support.entity.Report;
import com.yingjianxia.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 举报 Controller（前端兼容路径 /api/v1/reports）
 */
@Tag(name = "举报服务")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SupportService service;

    @Operation(summary = "举报列表")
    @GetMapping
    public ApiResponse<PageResult<Report>> list(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.listReports(pageNum, pageSize));
    }

    @Operation(summary = "提交举报")
    @PostMapping
    public ApiResponse<Long> createReport(@Valid @RequestBody ReportCreateReq req) {
        return ApiResponse.success(service.createReport(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "我的举报列表")
    @GetMapping("/mine")
    public ApiResponse<PageResult<Report>> mine(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.myReports(UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "[管理端] 处理举报")
    @PostMapping("/{reportId}/handle")
    public ApiResponse<Void> handleReport(@PathVariable Long reportId, @RequestParam String result) {
        service.handleReport(reportId, UserContext.requiredUserId(), result);
        return ApiResponse.success();
    }
}
