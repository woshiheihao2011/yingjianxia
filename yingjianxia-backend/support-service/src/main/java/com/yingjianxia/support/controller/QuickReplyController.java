package com.yingjianxia.support.controller;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.support.entity.QuickReply;
import com.yingjianxia.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 快捷回复 Controller（前端兼容路径 /api/v1/quick-replies）
 */
@Tag(name = "快捷回复服务")
@RestController
@RequestMapping("/api/v1/quick-replies")
@RequiredArgsConstructor
public class QuickReplyController {

    private final SupportService service;

    @Operation(summary = "快捷回复列表")
    @GetMapping
    public ApiResponse<List<QuickReply>> list(@RequestParam(required = false) Long shopId) {
        if (shopId == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        return ApiResponse.success(service.listQuickReplies(shopId));
    }

    @Operation(summary = "新增/更新快捷回复")
    @PostMapping
    public ApiResponse<Long> save(@RequestBody QuickReply req) {
        return ApiResponse.success(service.saveQuickReply(req));
    }
}
