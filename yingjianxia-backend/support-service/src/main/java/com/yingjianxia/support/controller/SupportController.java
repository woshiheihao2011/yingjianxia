package com.yingjianxia.support.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.support.dto.AnnouncementQueryReq;
import com.yingjianxia.support.dto.FaqQueryReq;
import com.yingjianxia.support.dto.ReportCreateReq;
import com.yingjianxia.support.dto.TicketCreateReq;
import com.yingjianxia.support.dto.TicketReplyReq;
import com.yingjianxia.support.entity.Announcement;
import com.yingjianxia.support.entity.Faq;
import com.yingjianxia.support.entity.QuickReply;
import com.yingjianxia.support.entity.Report;
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
 * 客服支撑服务 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "客服支撑服务", description = "工单/FAQ/公告/举报/快捷回复配置")
@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService service;

    /* ========== 工单 ========== */

    @Operation(summary = "提交工单")
    @PostMapping("/tickets")
    public ApiResponse<Long> createTicket(@Valid @RequestBody TicketCreateReq req) {
        return ApiResponse.success(service.createTicket(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "用户回复工单")
    @PostMapping("/tickets/reply")
    public ApiResponse<Void> userReply(@Valid @RequestBody TicketReplyReq req) {
        service.userReply(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[客服] 回复工单")
    @PostMapping("/tickets/agent-reply")
    public ApiResponse<Void> agentReply(@Valid @RequestBody TicketReplyReq req) {
        service.agentReply(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "工单详情")
    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<Ticket> getTicket(@PathVariable Long ticketId) {
        return ApiResponse.success(service.getTicketDetail(ticketId));
    }

    @Operation(summary = "工单沟通记录")
    @GetMapping("/tickets/{ticketId}/messages")
    public ApiResponse<List<TicketMessage>> listMessages(@PathVariable Long ticketId) {
        return ApiResponse.success(service.listTicketMessages(ticketId));
    }

    @Operation(summary = "我的工单列表")
    @GetMapping("/tickets/mine")
    public ApiResponse<PageResult<Ticket>> myTickets(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.myTickets(UserContext.requiredUserId(), status, pageNum, pageSize));
    }

    @Operation(summary = "用户关闭工单")
    @PostMapping("/tickets/{ticketId}/close")
    public ApiResponse<Void> closeTicket(@PathVariable Long ticketId) {
        service.closeTicketByUser(ticketId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[客服] 处理工单（状态流转）")
    @PostMapping("/tickets/{ticketId}/process")
    public ApiResponse<Void> processTicket(@PathVariable Long ticketId,
                                            @RequestParam Integer toStatus,
                                            @RequestParam(required = false) String remark) {
        service.processTicket(ticketId, UserContext.requiredUserId(), toStatus, remark);
        return ApiResponse.success();
    }

    /* ========== FAQ ========== */

    @Operation(summary = "FAQ列表")
    @GetMapping("/faqs")
    public ApiResponse<PageResult<Faq>> listFaqs(FaqQueryReq req) {
        return ApiResponse.success(service.listFaqs(req));
    }

    @Operation(summary = "FAQ详情")
    @GetMapping("/faqs/{id}")
    public ApiResponse<Faq> getFaq(@PathVariable Long id) {
        return ApiResponse.success(service.getFaqDetail(id));
    }

    /* ========== 公告 ========== */

    @Operation(summary = "公告列表（置顶优先）")
    @GetMapping("/announcements")
    public ApiResponse<PageResult<Announcement>> listAnnouncements(AnnouncementQueryReq req) {
        return ApiResponse.success(service.listAnnouncements(req));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/announcements/{id}")
    public ApiResponse<Announcement> getAnnouncement(@PathVariable Long id) {
        return ApiResponse.success(service.getAnnouncementDetail(id));
    }

    /* ========== 举报 ========== */

    @Operation(summary = "提交举报")
    @PostMapping("/reports")
    public ApiResponse<Long> createReport(@Valid @RequestBody ReportCreateReq req) {
        return ApiResponse.success(service.createReport(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "我的举报列表")
    @GetMapping("/reports/mine")
    public ApiResponse<PageResult<Report>> myReports(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.success(service.myReports(UserContext.requiredUserId(), pageNum, pageSize));
    }

    @Operation(summary = "[管理端] 处理举报")
    @PostMapping("/reports/{reportId}/handle")
    public ApiResponse<Void> handleReport(@PathVariable Long reportId, @RequestParam String result) {
        service.handleReport(reportId, UserContext.requiredUserId(), result);
        return ApiResponse.success();
    }

    /* ========== 客服端：快捷回复配置 ========== */

    @Operation(summary = "[客服端] 快捷回复配置列表")
    @GetMapping("/quick-replies")
    public ApiResponse<List<QuickReply>> listQuickReplies(@RequestParam Long shopId) {
        return ApiResponse.success(service.listQuickReplies(shopId));
    }

    @Operation(summary = "[客服端] 新增/更新快捷回复配置")
    @PostMapping("/quick-replies")
    public ApiResponse<Long> saveQuickReply(@RequestBody QuickReply req) {
        return ApiResponse.success(service.saveQuickReply(req));
    }
}
