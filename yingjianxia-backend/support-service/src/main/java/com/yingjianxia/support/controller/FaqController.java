package com.yingjianxia.support.controller;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.support.dto.FaqQueryReq;
import com.yingjianxia.support.entity.Faq;
import com.yingjianxia.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * FAQ Controller（前端兼容路径 /api/v1/faqs）
 */
@Tag(name = "FAQ服务")
@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final SupportService service;

    @Operation(summary = "FAQ列表")
    @GetMapping
    public ApiResponse<PageResult<Faq>> list(FaqQueryReq req) {
        return ApiResponse.success(service.listFaqs(req));
    }

    @Operation(summary = "FAQ详情")
    @GetMapping("/{id}")
    public ApiResponse<Faq> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getFaqDetail(id));
    }
}
