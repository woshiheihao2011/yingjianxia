package com.yingjianxia.support.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.exception.BusinessException;
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
import com.yingjianxia.support.enums.SupportErrorCode;
import com.yingjianxia.support.mapper.AnnouncementMapper;
import com.yingjianxia.support.mapper.FaqMapper;
import com.yingjianxia.support.mapper.QuickReplyMapper;
import com.yingjianxia.support.mapper.ReportMapper;
import com.yingjianxia.support.mapper.TicketMapper;
import com.yingjianxia.support.mapper.TicketMessageMapper;
import com.yingjianxia.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客服支撑服务实现 — 工单系统/快捷回复/FAQ/公告/举报
 * <p>
 * 工单状态流转：0待处理 → 1处理中 → 2待用户回复 → 3已解决 → 4已关闭
 * 待用户回复 7 天自动关闭。
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper messageMapper;
    private final QuickReplyMapper quickReplyMapper;
    private final FaqMapper faqMapper;
    private final AnnouncementMapper announcementMapper;
    private final ReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    /** 待用户回复自动关闭天数 */
    private static final int AUTO_CLOSE_DAYS = 7;

    /* ======================== 工单 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketCreateReq req, Long userId) {
        Ticket ticket = new Ticket();
        ticket.setTicketNo("TK" + IdUtil.getSnowflakeNextIdStr());
        ticket.setUserId(userId);
        ticket.setOrderId(req.getOrderId());
        ticket.setProblemType(req.getProblemType());
        ticket.setPriority(req.getPriority() == null ? 2 : req.getPriority());
        ticket.setTitle(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setImages(toJson(req.getImages()));
        ticket.setContact(req.getContact());
        ticket.setStatus(Ticket.STATUS_PENDING);
        ticket.setAgentGroup("default");
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticketMapper.insert(ticket);

        // 自动分配客服占位 — 生产环境对接客服路由/排队系统
        Long assignedAgent = assignAgent(ticket);
        if (assignedAgent != null) {
            ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                    .eq(Ticket::getId, ticket.getId())
                    .set(Ticket::getAgentId, assignedAgent)
                    .set(Ticket::getStatus, Ticket.STATUS_PROCESSING)
                    .set(Ticket::getUpdatedAt, now));
            // 系统消息：已为您分配客服
            sendSystemMessage(ticket.getId(), "您的工单已分配客服 #" + assignedAgent + "，请耐心等待处理。");
        } else {
            sendSystemMessage(ticket.getId(), "您的工单已提交，客服将在工作时间内尽快处理。");
        }
        log.info("【提交工单】userId={}, ticketNo={}, agentId={}", userId, ticket.getTicketNo(), assignedAgent);
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userReply(TicketReplyReq req, Long userId) {
        Ticket ticket = requireTicket(req.getTicketId());
        if (ticket.getStatus() == Ticket.STATUS_CLOSED) {
            throw new BusinessException(SupportErrorCode.TICKET_ALREADY_CLOSED);
        }
        if (ticket.getStatus() == Ticket.STATUS_RESOLVED) {
            throw new BusinessException(SupportErrorCode.TICKET_REPLY_FORBIDDEN);
        }
        if (!ticket.getUserId().equals(userId)) {
            throw new BusinessException(SupportErrorCode.TICKET_NO_PERMISSION);
        }
        saveMessage(req.getTicketId(), TicketMessage.SENDER_USER, userId, String.valueOf(userId),
                req.getContent(), req.getImages(), false);
        // 用户回复后状态回到处理中，清空自动关闭时间
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, req.getTicketId())
                .set(Ticket::getStatus, Ticket.STATUS_PROCESSING)
                .set(Ticket::getAutoCloseAt, null)
                .set(Ticket::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agentReply(TicketReplyReq req, Long agentId) {
        Ticket ticket = requireTicket(req.getTicketId());
        if (ticket.getStatus() == Ticket.STATUS_CLOSED) {
            throw new BusinessException(SupportErrorCode.TICKET_ALREADY_CLOSED);
        }
        if (ticket.getAgentId() == null || !ticket.getAgentId().equals(agentId)) {
            throw new BusinessException(SupportErrorCode.TICKET_NO_PERMISSION);
        }
        saveMessage(req.getTicketId(), TicketMessage.SENDER_AGENT, agentId, "客服" + agentId,
                req.getContent(), req.getImages(), false);
        // 客服回复后转入待用户回复，并设置 7 天后自动关闭
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, req.getTicketId())
                .set(Ticket::getStatus, Ticket.STATUS_WAIT_USER)
                .set(Ticket::getAutoCloseAt, LocalDateTime.now().plusDays(AUTO_CLOSE_DAYS))
                .set(Ticket::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public Ticket getTicketDetail(Long ticketId) {
        return requireTicket(ticketId);
    }

    @Override
    public List<TicketMessage> listTicketMessages(Long ticketId) {
        return messageMapper.selectList(new LambdaQueryWrapper<TicketMessage>()
                .eq(TicketMessage::getTicketId, ticketId)
                .eq(TicketMessage::getIsInternal, 0)
                .orderByAsc(TicketMessage::getCreatedAt));
    }

    @Override
    public PageResult<Ticket> myTickets(Long userId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getUserId, userId)
                .eq(status != null, Ticket::getStatus, status)
                .orderByDesc(Ticket::getCreatedAt);
        IPage<Ticket> page = ticketMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTicketByUser(Long ticketId, Long userId) {
        Ticket ticket = requireTicket(ticketId);
        if (!ticket.getUserId().equals(userId)) {
            throw new BusinessException(SupportErrorCode.TICKET_NO_PERMISSION);
        }
        if (ticket.getStatus() == Ticket.STATUS_CLOSED) {
            throw new BusinessException(SupportErrorCode.TICKET_ALREADY_CLOSED);
        }
        LocalDateTime now = LocalDateTime.now();
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticketId)
                .set(Ticket::getStatus, Ticket.STATUS_CLOSED)
                .set(Ticket::getClosedAt, now)
                .set(Ticket::getUpdatedAt, now));
        sendSystemMessage(ticketId, "用户已主动关闭工单。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenTicket(Long ticketId, Long userId) {
        Ticket ticket = requireTicket(ticketId);
        if (!ticket.getUserId().equals(userId)) {
            throw new BusinessException(SupportErrorCode.TICKET_NO_PERMISSION);
        }
        if (ticket.getStatus() != Ticket.STATUS_CLOSED) {
            throw new BusinessException(SupportErrorCode.TICKET_STATUS_INVALID);
        }
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticketId)
                .set(Ticket::getStatus, Ticket.STATUS_PROCESSING)
                .set(Ticket::getClosedAt, null)
                .set(Ticket::getUpdatedAt, LocalDateTime.now()));
        sendSystemMessage(ticketId, "用户已重新开启工单。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long ticketId, Long agentId, Integer toStatus, String remark) {
        Ticket ticket = requireTicket(ticketId);
        if (ticket.getAgentId() == null || !ticket.getAgentId().equals(agentId)) {
            throw new BusinessException(SupportErrorCode.TICKET_NO_PERMISSION);
        }
        if (!isValidTransition(ticket.getStatus(), toStatus)) {
            throw new BusinessException(SupportErrorCode.TICKET_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Ticket> uw = new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticketId)
                .set(Ticket::getStatus, toStatus)
                .set(Ticket::getUpdatedAt, now);
        if (toStatus == Ticket.STATUS_RESOLVED) {
            uw.set(Ticket::getResolvedAt, now);
        } else if (toStatus == Ticket.STATUS_CLOSED) {
            uw.set(Ticket::getClosedAt, now);
        } else if (toStatus == Ticket.STATUS_WAIT_USER) {
            uw.set(Ticket::getAutoCloseAt, now.plusDays(AUTO_CLOSE_DAYS));
        }
        ticketMapper.update(null, uw);
        if (StrUtil.isNotBlank(remark)) {
            saveMessage(ticketId, TicketMessage.SENDER_SYSTEM, 0L, "系统",
                    remark, null, false);
        }
        log.info("【工单流转】ticketId={}, {} -> {}, agentId={}", ticketId, ticket.getStatus(), toStatus, agentId);
    }

    /** 每分钟扫描待用户回复且到期的工单，自动关闭 */
    @Scheduled(fixedDelay = 60_000L)
    public void autoCloseTickets() {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getStatus, Ticket.STATUS_WAIT_USER)
                .le(Ticket::getAutoCloseAt, LocalDateTime.now());
        List<Ticket> list = ticketMapper.selectList(qw);
        if (list.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (Ticket t : list) {
            try {
                ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                        .eq(Ticket::getId, t.getId())
                        .set(Ticket::getStatus, Ticket.STATUS_CLOSED)
                        .set(Ticket::getClosedAt, now)
                        .set(Ticket::getUpdatedAt, now));
                sendSystemMessage(t.getId(), "工单因用户超过7天未回复，已自动关闭。");
            } catch (Exception e) {
                log.warn("工单自动关闭失败 ticketId={}", t.getId(), e);
            }
        }
        log.info("【工单自动关闭】本次关闭 {} 单", list.size());
    }

    /* ======================== 快捷回复 ======================== */

    @Override
    public List<QuickReply> listQuickReplies(Long shopId) {
        return quickReplyMapper.selectList(new LambdaQueryWrapper<QuickReply>()
                .eq(QuickReply::getShopId, shopId)
                .orderByAsc(QuickReply::getSortOrder)
                .orderByDesc(QuickReply::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveQuickReply(QuickReply req) {
        LocalDateTime now = LocalDateTime.now();
        if (req.getId() == null) {
            if (req.getIsEnabled() == null) req.setIsEnabled(1);
            if (req.getTriggerInterval() == null) req.setTriggerInterval(24);
            if (req.getUseCount() == null) req.setUseCount(0);
            if (req.getSortOrder() == null) req.setSortOrder(0);
            req.setCreatedAt(now);
            req.setUpdatedAt(now);
            quickReplyMapper.insert(req);
        } else {
            req.setUpdatedAt(now);
            quickReplyMapper.updateById(req);
        }
        return req.getId();
    }

    /* ======================== FAQ ======================== */

    @Override
    public PageResult<Faq> listFaqs(FaqQueryReq req) {
        LambdaQueryWrapper<Faq> qw = new LambdaQueryWrapper<Faq>()
                .eq(Faq::getStatus, Faq.STATUS_ENABLED)
                .eq(StrUtil.isNotBlank(req.getCategory()), Faq::getCategory, req.getCategory())
                .like(StrUtil.isNotBlank(req.getKeyword()), Faq::getQuestion, req.getKeyword())
                .orderByAsc(Faq::getSortOrder)
                .orderByDesc(Faq::getHelpfulCount);
        IPage<Faq> page = faqMapper.selectPage(new Page<>(req.getPageNum(), req.getPageSize()), qw);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public Faq getFaqDetail(Long id) {
        Faq faq = faqMapper.selectById(id);
        if (faq == null) {
            throw new BusinessException(SupportErrorCode.FAQ_NOT_FOUND);
        }
        if (faq.getStatus() != Faq.STATUS_ENABLED) {
            throw new BusinessException(SupportErrorCode.FAQ_DISABLED);
        }
        // 浏览量 +1
        faqMapper.update(null, new LambdaUpdateWrapper<Faq>()
                .eq(Faq::getId, id).setSql("view_count = view_count + 1"));
        return faq;
    }

    /* ======================== 公告 ======================== */

    @Override
    public PageResult<Announcement> listAnnouncements(AnnouncementQueryReq req) {
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, Announcement.STATUS_PUBLISHED)
                .eq(req.getCategory() != null, Announcement::getCategory, req.getCategory())
                .like(StrUtil.isNotBlank(req.getKeyword()), Announcement::getTitle, req.getKeyword())
                .orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishedAt);
        IPage<Announcement> page = announcementMapper.selectPage(new Page<>(req.getPageNum(), req.getPageSize()), qw);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public Announcement getAnnouncementDetail(Long id) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException(SupportErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }
        if (ann.getStatus() != Announcement.STATUS_PUBLISHED) {
            throw new BusinessException(SupportErrorCode.ANNOUNCEMENT_DRAFT);
        }
        announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id).setSql("view_count = view_count + 1"));
        return ann;
    }

    /* ======================== 举报 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(ReportCreateReq req, Long reporterId) {
        if (req.getTargetType() == null || req.getTargetType() < 1 || req.getTargetType() > 4) {
            throw new BusinessException(SupportErrorCode.TARGET_TYPE_INVALID);
        }
        if (req.getReasonType() == null || req.getReasonType() < 1 || req.getReasonType() > 6) {
            throw new BusinessException(SupportErrorCode.REASON_TYPE_INVALID);
        }
        // 防重：同一用户对同一对象重复举报
        Long cnt = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, req.getTargetType())
                .eq(Report::getTargetId, req.getTargetId())
                .ne(Report::getStatus, Report.STATUS_HANDLED));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(SupportErrorCode.REPORT_DUPLICATE);
        }
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setReasonType(req.getReasonType());
        report.setDescription(req.getDescription());
        report.setEvidenceImages(toJson(req.getEvidenceImages()));
        report.setIsAnonymous(Boolean.TRUE.equals(req.getIsAnonymous()) ? 1 : 0);
        report.setStatus(Report.STATUS_ACCEPTING);
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);
        log.info("【提交举报】reporterId={}, targetType={}, targetId={}", reporterId, req.getTargetType(), req.getTargetId());
        return report.getId();
    }

    @Override
    public PageResult<Report> myReports(Long reporterId, long pageNum, long pageSize) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .orderByDesc(Report::getCreatedAt);
        IPage<Report> page = reportMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<Report> listReports(long pageNum, long pageSize) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getCreatedAt);
        IPage<Report> page = reportMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, Long handlerId, String result) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(SupportErrorCode.REPORT_NOT_FOUND);
        }
        if (report.getStatus() == Report.STATUS_HANDLED) {
            throw new BusinessException(SupportErrorCode.REPORT_ALREADY_HANDLED);
        }
        reportMapper.update(null, new LambdaUpdateWrapper<Report>()
                .eq(Report::getId, reportId)
                .set(Report::getStatus, Report.STATUS_HANDLED)
                .set(Report::getHandlerId, handlerId)
                .set(Report::getResult, result)
                .set(Report::getHandledAt, LocalDateTime.now()));
        log.info("【处理举报】reportId={}, handlerId={}", reportId, handlerId);
    }

    /* ======================== 内部工具 ======================== */

    private Ticket requireTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(SupportErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    /** 自动分配客服占位 — 生产环境对接客服路由/排队系统 */
    private Long assignAgent(Ticket ticket) {
        // TODO: 对接客服路由系统，按 agent_group、空闲度分配；此处占位返回固定客服
        return 10001L;
    }

    private void saveMessage(Long ticketId, int senderType, Long senderId, String senderName,
                             String content, List<String> images, boolean internal) {
        TicketMessage msg = new TicketMessage();
        msg.setTicketId(ticketId);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId == null ? 0L : senderId);
        msg.setSenderName(senderName);
        msg.setContent(content);
        msg.setImages(toJson(images));
        msg.setIsInternal(internal ? 1 : 0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    private void sendSystemMessage(Long ticketId, String content) {
        saveMessage(ticketId, TicketMessage.SENDER_SYSTEM, 0L, "系统", content, null, false);
    }

    private boolean isValidTransition(Integer from, Integer to) {
        if (from == null || to == null) return false;
        // 0->1, 1->2, 1->3, 2->1, 2->3, 2->4, 3->4
        return switch (from) {
            case Ticket.STATUS_PENDING -> to == Ticket.STATUS_PROCESSING || to == Ticket.STATUS_RESOLVED || to == Ticket.STATUS_CLOSED;
            case Ticket.STATUS_PROCESSING -> to == Ticket.STATUS_WAIT_USER || to == Ticket.STATUS_RESOLVED || to == Ticket.STATUS_CLOSED;
            case Ticket.STATUS_WAIT_USER -> to == Ticket.STATUS_PROCESSING || to == Ticket.STATUS_RESOLVED || to == Ticket.STATUS_CLOSED;
            case Ticket.STATUS_RESOLVED -> to == Ticket.STATUS_CLOSED;
            default -> false;
        };
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
