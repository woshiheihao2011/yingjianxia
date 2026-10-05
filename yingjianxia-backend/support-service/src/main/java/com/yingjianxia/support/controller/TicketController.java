package com.yingjianxia.support.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.support.dto.TicketCreateReq;
import com.yingjianxia.support.dto.TicketReplyReq;
import com.yingjianxia.support.entity.Ticket;
import com.yingjianxia.support.entity.TicketMessage;
import com.yingjianxia.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单 Controller（前端兼容路径 /api/v1/tickets）
 */
@Tag(name = "工单服务")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final SupportService service;

    @Operation(summary = "提交工单")
    @PostMapping
    public ApiResponse<Long> createTicket(@Valid @RequestBody TicketCreateReq req) {
        return ApiResponse.success(service.createTicket(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "我的工单列表")
    @GetMapping("/mine")
    public ApiResponse<PageResult<Ticket>> mine(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.myTickets(UserContext.requiredUserId(), status, pageNum, pageSize));
    }

    @Operation(summary = "工单列表")
    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(service.myTickets(UserContext.requiredUserId(), null, 1, 20));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{ticketId}")
    public ApiResponse<Ticket> detail(@PathVariable Long ticketId) {
        return ApiResponse.success(service.getTicketDetail(ticketId));
    }

    @Operation(summary = "工单沟通记录")
    @GetMapping("/{ticketId}/messages")
    public ApiResponse<List<TicketMessage>> messages(@PathVariable Long ticketId) {
        return ApiResponse.success(service.listTicketMessages(ticketId));
    }

    @Operation(summary = "回复工单")
    @PostMapping("/{ticketId}/reply")
    public ApiResponse<Void> reply(@PathVariable Long ticketId, @Valid @RequestBody TicketReplyReq req) {
        req.setTicketId(ticketId);
        service.userReply(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "关闭工单")
    @PostMapping("/{ticketId}/close")
    public ApiResponse<Void> close(@PathVariable Long ticketId) {
        service.closeTicketByUser(ticketId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "重开工单")
    @PostMapping("/{ticketId}/reopen")
    public ApiResponse<Void> reopen(@PathVariable Long ticketId) {
        service.reopenTicket(ticketId, UserContext.requiredUserId());
        return ApiResponse.success();
    }
}
