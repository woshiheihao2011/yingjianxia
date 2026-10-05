package com.yingjianxia.support.controller;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.support.dto.AnnouncementQueryReq;
import com.yingjianxia.support.entity.Announcement;
import com.yingjianxia.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公告 Controller（前端兼容路径 /api/v1/announcements）
 */
@Tag(name = "公告服务")
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final SupportService service;

    @Operation(summary = "公告列表")
    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(service.listAnnouncements(new AnnouncementQueryReq()));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public ApiResponse<Announcement> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getAnnouncementDetail(id));
    }
}
