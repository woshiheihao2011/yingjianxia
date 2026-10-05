package com.yingjianxia.support.service;

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

import java.util.List;

/**
 * 客服支撑服务接口
 *
 * @author 硬件侠后端团队
 */
public interface SupportService {

    /* ========== 工单 ========== */

    /** 提交工单（自动分配客服占位） */
    Long createTicket(TicketCreateReq req, Long userId);

    /** 用户回复工单 */
    void userReply(TicketReplyReq req, Long userId);

    /** 客服回复工单 */
    void agentReply(TicketReplyReq req, Long agentId);

    /** 工单详情 */
    Ticket getTicketDetail(Long ticketId);

    /** 工单沟通记录 */
    List<TicketMessage> listTicketMessages(Long ticketId);

    /** 我的工单列表 */
    PageResult<Ticket> myTickets(Long userId, Integer status, long pageNum, long pageSize);

    /** 用户关闭工单 */
    void closeTicketByUser(Long ticketId, Long userId);

    /** 用户重开工单（已关闭 → 处理中） */
    void reopenTicket(Long ticketId, Long userId);

    /** 客服处理工单（状态流转） */
    void processTicket(Long ticketId, Long agentId, Integer toStatus, String remark);

    /* ========== 快捷回复 ========== */

    /** 快捷回复配置列表 */
    List<QuickReply> listQuickReplies(Long shopId);

    /** 新增/更新快捷回复配置 */
    Long saveQuickReply(QuickReply req);

    /* ========== FAQ ========== */

    /** FAQ列表 */
    PageResult<Faq> listFaqs(FaqQueryReq req);

    /** FAQ详情 */
    Faq getFaqDetail(Long id);

    /* ========== 公告 ========== */

    /** 公告列表（置顶优先） */
    PageResult<Announcement> listAnnouncements(AnnouncementQueryReq req);

    /** 公告详情 */
    Announcement getAnnouncementDetail(Long id);

    /* ========== 举报 ========== */

    /** 提交举报 */
    Long createReport(ReportCreateReq req, Long reporterId);

    /** 我的举报列表 */
    PageResult<Report> myReports(Long reporterId, long pageNum, long pageSize);

    /** 举报列表（管理端） */
    PageResult<Report> listReports(long pageNum, long pageSize);

    /** 处理举报 */
    void handleReport(Long reportId, Long handlerId, String result);
}
