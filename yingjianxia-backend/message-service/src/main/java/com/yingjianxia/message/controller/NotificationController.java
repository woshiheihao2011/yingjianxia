package com.yingjianxia.message.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知服务 Controller（系统通知）
 * <p>
 * 通知类型：订单通知、验机通知、审核通知、系统公告等
 */
@Tag(name = "通知服务", description = "系统通知列表/已读/未读数")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @Operation(summary = "通知列表（分页）")
    @GetMapping
    public ApiResponse<PageResult<NotificationResp>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        // TODO: 接入真实通知表（db_message.notifications）后替换为 Service 查询
        // 当前返回空列表，前端可正常渲染"暂无通知"
        List<NotificationResp> list = new ArrayList<>();
        return ApiResponse.success(PageResult.of(page.longValue(), size.longValue(), 0L, list));
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        // TODO: 从 Redis/DB 获取当前用户未读通知数
        return ApiResponse.success(0);
    }

    @Operation(summary = "标记通知已读")
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        // TODO: 更新通知状态为已读
        return ApiResponse.success();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        // TODO: 将当前用户所有通知标记为已读
        return ApiResponse.success();
    }

    @Data
    public static class NotificationResp {
        private Long id;
        private String type;
        private String title;
        private String content;
        private Boolean read;
        private LocalDateTime createdAt;
    }
}
