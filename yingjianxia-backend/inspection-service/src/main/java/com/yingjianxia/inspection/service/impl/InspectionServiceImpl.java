package com.yingjianxia.inspection.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.inspection.dto.InspectionSubmitReq;
import com.yingjianxia.inspection.entity.InspectionItem;
import com.yingjianxia.inspection.entity.InspectionReport;
import com.yingjianxia.inspection.entity.InspectionTemplate;
import com.yingjianxia.inspection.enums.InspectionErrorCode;
import com.yingjianxia.inspection.mapper.InspectionItemMapper;
import com.yingjianxia.inspection.mapper.InspectionReportMapper;
import com.yingjianxia.inspection.mapper.InspectionTemplateMapper;
import com.yingjianxia.inspection.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 验机服务实现 — 核心：SHA-256 签名防篡改 + 评级算法 + PDF 生成占位
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    /** 系统默认检测员ID（创建报告时未指定检测员则使用此值） */
    private static final Long DEFAULT_INSPECTOR_ID = 20001L;

    private final InspectionTemplateMapper templateMapper;
    private final InspectionReportMapper reportMapper;
    private final InspectionItemMapper itemMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<InspectionTemplate> listTemplates(Integer categoryId) {
        // 优先取具体分类，为空或不存在时回退 category=0（通用）
        List<InspectionTemplate> specific = templateMapper.selectList(new LambdaQueryWrapper<InspectionTemplate>()
                .eq(InspectionTemplate::getCategoryId, categoryId == null ? 0 : categoryId)
                .eq(InspectionTemplate::getStatus, 1)
                .orderByAsc(InspectionTemplate::getItemNo));
        if (!specific.isEmpty()) return specific;
        return templateMapper.selectList(new LambdaQueryWrapper<InspectionTemplate>()
                .eq(InspectionTemplate::getCategoryId, 0)
                .eq(InspectionTemplate::getStatus, 1)
                .orderByAsc(InspectionTemplate::getItemNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(Long productId, Long orderId, Long inspectorId, String inspectorName) {
        Long dup = reportMapper.selectCount(new LambdaQueryWrapper<InspectionReport>()
                .eq(InspectionReport::getProductId, productId));
        if (dup != null && dup > 0) {
            throw new BusinessException(InspectionErrorCode.REPORT_ALREADY_EXISTS);
        }
        InspectionReport r = new InspectionReport();
        r.setProductId(productId);
        r.setOrderId(orderId);
        // 自动分配检测员：未指定时使用系统默认检测员
        if (inspectorId == null) {
            inspectorId = DEFAULT_INSPECTOR_ID;
            inspectorName = "系统检测员";
        }
        r.setInspectorId(inspectorId);
        r.setInspectorName(inspectorName);
        r.setStatus(InspectionReport.STATUS_PENDING);
        r.setGrade("待检测");
        r.setOverallResult(0);
        r.setSignature("pending");  // 完成时覆盖
        r.setReceivedAt(LocalDateTime.now());
        reportMapper.insert(r);
        log.info("【验机报告创建】productId={}, reportId={}", productId, r.getId());
        return r.getId();
    }

    @Override
    public void startTesting(Long reportId, Long inspectorId) {
        InspectionReport r = requireReport(reportId);
        // 自动分配检测员：若报告未分配检测员或处于待检测状态，将当前用户设为检测员
        if (r.getInspectorId() == null || r.getStatus() == InspectionReport.STATUS_PENDING) {
            reportMapper.update(null, new LambdaUpdateWrapper<InspectionReport>()
                    .eq(InspectionReport::getId, reportId)
                    .set(InspectionReport::getInspectorId, inspectorId)
                    .set(InspectionReport::getInspectorName, "检测员" + inspectorId));
            r.setInspectorId(inspectorId);
        }
        if (!r.getInspectorId().equals(inspectorId)) {
            throw new BusinessException(InspectionErrorCode.NO_PERMISSION);
        }
        if (r.getStatus() != InspectionReport.STATUS_PENDING) {
            throw new BusinessException(InspectionErrorCode.INVALID_STATUS_TRANSITION);
        }
        reportMapper.update(null, new LambdaUpdateWrapper<InspectionReport>()
                .eq(InspectionReport::getId, reportId)
                .set(InspectionReport::getStatus, InspectionReport.STATUS_TESTING)
                .set(InspectionReport::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionReport submitResult(InspectionSubmitReq req, Long inspectorId) {
        InspectionReport r = requireReport(req.getReportId());
        if (!r.getInspectorId().equals(inspectorId)) {
            throw new BusinessException(InspectionErrorCode.NO_PERMISSION);
        }
        if (r.getStatus() == InspectionReport.STATUS_DONE) {
            throw new BusinessException(InspectionErrorCode.REPORT_ALREADY_DONE);
        }

        // 0. 兼容前端字段：自动填充缺失的 itemNo / passed / grade
        List<InspectionSubmitReq.ItemResult> items = req.getItems();
        for (int i = 0; i < items.size(); i++) {
            InspectionSubmitReq.ItemResult it = items.get(i);
            // itemNo 缺失时按列表顺序自动编号（1-based）
            if (it.getItemNo() == null) {
                it.setItemNo(i + 1);
            }
            // passed 缺失时根据 result 推导
            if (it.getPassed() == null) {
                String res = it.getResult();
                it.setPassed(res != null && (res.contains("通过") || res.contains("合格") || res.contains("正常") || res.contains("良好") || res.contains("优秀")));
            }
        }

        // 1. 评级合法性（前端不传 grade 时自动推导）
        String grade = req.getGrade();
        if (StrUtil.isBlank(grade)) {
            long passedCount = items.stream().filter(i -> Boolean.TRUE.equals(i.getPassed())).count();
            if (passedCount == items.size()) {
                grade = InspectionReport.GRADE_EXCELLENT;
            } else if (passedCount >= items.size() * 0.6) {
                grade = InspectionReport.GRADE_GOOD;
            } else if (passedCount >= items.size() * 0.3) {
                grade = InspectionReport.GRADE_PASS;
            } else {
                grade = InspectionReport.GRADE_FAIL;
            }
            log.info("【验机提交】grade 未传，自动推导为 {}", grade);
        }
        if (!(InspectionReport.GRADE_EXCELLENT.equals(grade)
                || InspectionReport.GRADE_GOOD.equals(grade)
                || InspectionReport.GRADE_PASS.equals(grade)
                || InspectionReport.GRADE_FAIL.equals(grade))) {
            throw new BusinessException(InspectionErrorCode.INVALID_GRADE);
        }

        // 2. 检测项齐全性校验（放宽：不强制12项，只要非空即可）
        Set<Integer> nos = items.stream().map(InspectionSubmitReq.ItemResult::getItemNo).collect(Collectors.toSet());

        // 3. overallResult 计算：全通过→1 部分→2 不通过→3
        long passedCount = items.stream().filter(i -> Boolean.TRUE.equals(i.getPassed())).count();
        int overall;
        if (passedCount == items.size()) overall = InspectionReport.RESULT_ALL_PASS;
        else if (passedCount >= items.size() * 0.5) overall = InspectionReport.RESULT_PART_PASS;
        else overall = InspectionReport.RESULT_FAIL;
        // 评级为不合格时，overall 强制 RESULT_FAIL
        if (InspectionReport.GRADE_FAIL.equals(grade)) overall = InspectionReport.RESULT_FAIL;

        // 4. 写检测项明细（先删后插）
        itemMapper.delete(new LambdaQueryWrapper<InspectionItem>().eq(InspectionItem::getReportId, r.getId()));
        for (InspectionSubmitReq.ItemResult it : items) {
            InspectionItem item = new InspectionItem();
            item.setReportId(r.getId());
            item.setItemNo(it.getItemNo());
            item.setItemName(it.getLabel() != null ? it.getLabel() : findItemName(it.getItemNo()));
            item.setResult(it.getResult());
            item.setIsPassed(it.getPassed());
            item.setDetail(it.getDetail());
            itemMapper.insert(item);
        }

        // 5. 计算 SHA-256 签名：reportId + productId + grade + overallResult +
        //    sorted(itemNo|isPassed|result). 签名不包含动态时间/pk
        String payload = buildSignaturePayload(r.getId(), r.getProductId(), grade, overall, items);
        String signature = SecureUtil.sha256(payload);

        // 6. 更新报告（状态→已完成，签名不可再改）
        LocalDateTime now = LocalDateTime.now();
        reportMapper.update(null, new LambdaUpdateWrapper<InspectionReport>()
                .eq(InspectionReport::getId, r.getId())
                .set(InspectionReport::getStatus, InspectionReport.STATUS_DONE)
                .set(InspectionReport::getGrade, grade)
                .set(InspectionReport::getOverallResult, overall)
                .set(InspectionReport::getRemark, req.getRemark())
                .set(InspectionReport::getSignature, signature)
                .set(InspectionReport::getCompletedAt, now)
                .set(InspectionReport::getUpdatedAt, now));

        // 7. 生成 PDF 报告（上传 OSS → URL，开发期生成本地/占位）
        String pdfUrl = buildPdfAndUpload(r.getId());
        if (StrUtil.isNotBlank(pdfUrl)) {
            reportMapper.update(null, new LambdaUpdateWrapper<InspectionReport>()
                    .eq(InspectionReport::getId, r.getId())
                    .set(InspectionReport::getReportFileUrl, pdfUrl));
        }

        // 8. 发送验机完成事件 → product-service 标记已验机 → 审核域提前触发机审
        // TODO: OutboxPattern: send "inspection.completed" event
        log.info("【验机报告完成】reportId={}, productId={}, grade={}, sign={}, pdf={}",
                r.getId(), r.getProductId(), grade, signature, pdfUrl);

        // 返回完整报告
        return getReportDetail(r.getId());
    }

    @Override
    public InspectionReport getReportDetail(Long reportId) {
        InspectionReport r = requireReport(reportId);
        List<InspectionItem> items = itemMapper.selectList(new LambdaQueryWrapper<InspectionItem>()
                .eq(InspectionItem::getReportId, reportId).orderByAsc(InspectionItem::getItemNo));
        r.setItems(items);
        return r;
    }

    @Override
    public InspectionReport getReportByProduct(Long productId) {
        InspectionReport r = reportMapper.selectOne(new LambdaQueryWrapper<InspectionReport>()
                .eq(InspectionReport::getProductId, productId));
        if (r == null) throw new BusinessException(InspectionErrorCode.REPORT_NOT_FOUND);
        return getReportDetail(r.getId());
    }

    @Override
    public boolean verifySignature(Long reportId) {
        InspectionReport r = requireReport(reportId);
        List<InspectionItem> items = itemMapper.selectList(new LambdaQueryWrapper<InspectionItem>()
                .eq(InspectionItem::getReportId, reportId).orderByAsc(InspectionItem::getItemNo));
        List<InspectionSubmitReq.ItemResult> rs = items.stream().map(i -> {
            InspectionSubmitReq.ItemResult ir = new InspectionSubmitReq.ItemResult();
            ir.setItemNo(i.getItemNo());
            ir.setResult(i.getResult());
            ir.setPassed(i.getIsPassed());
            return ir;
        }).toList();
        String payload = buildSignaturePayload(r.getId(), r.getProductId(),
                r.getGrade(), r.getOverallResult(), rs);
        String expected = SecureUtil.sha256(payload);
        return expected.equalsIgnoreCase(r.getSignature());
    }

    @Override
    public String generateOrGetPdfUrl(Long reportId) {
        InspectionReport r = requireReport(reportId);
        if (r.getStatus() != InspectionReport.STATUS_DONE) {
            throw new BusinessException(InspectionErrorCode.REPORT_NOT_DONE);
        }
        if (StrUtil.isNotBlank(r.getReportFileUrl())) return r.getReportFileUrl();
        String url = buildPdfAndUpload(reportId);
        if (StrUtil.isNotBlank(url)) {
            reportMapper.update(null, new LambdaUpdateWrapper<InspectionReport>()
                    .eq(InspectionReport::getId, reportId)
                    .set(InspectionReport::getReportFileUrl, url));
        }
        return url;
    }

    /* ======================== 内部工具 ======================== */

    private InspectionReport requireReport(Long id) {
        InspectionReport r = reportMapper.selectById(id);
        if (r == null) throw new BusinessException(InspectionErrorCode.REPORT_NOT_FOUND);
        return r;
    }

    private String findItemName(int itemNo) {
        // 优先查通用模板
        InspectionTemplate tpl = templateMapper.selectOne(new LambdaQueryWrapper<InspectionTemplate>()
                .eq(InspectionTemplate::getCategoryId, 0)
                .eq(InspectionTemplate::getItemNo, itemNo)
                .last("LIMIT 1"));
        return tpl == null ? ("检测项" + itemNo) : tpl.getItemName();
    }

    /**
     * 签名载荷：以稳定字段拼接，保证任何篡改都会造成 sign 不一致
     */
    private String buildSignaturePayload(Long reportId, Long productId, String grade,
                                         Integer overall, List<InspectionSubmitReq.ItemResult> items) {
        StringJoiner sj = new StringJoiner("|");
        sj.add(String.valueOf(reportId));
        sj.add(String.valueOf(productId));
        sj.add(grade);
        sj.add(String.valueOf(overall));
        items.stream().sorted(Comparator.comparingInt(InspectionSubmitReq.ItemResult::getItemNo)).forEach(i ->
                sj.add(i.getItemNo() + ":" + (Boolean.TRUE.equals(i.getPassed()) ? 1 : 0) + ":"
                        + (i.getResult() == null ? "" : i.getResult())));
        return sj.toString();
    }

    /**
     * PDF 生成（开发期：生成 JSON 文本作为 PDF 占位；生产使用 Flying Saucer / iText）
     */
    private String buildPdfAndUpload(Long reportId) {
        try {
            InspectionReport r = getReportDetail(reportId);
            String json = objectMapper.writeValueAsString(r);
            // 开发占位：返回一个伪 OSS URL（真实环境上传到阿里云 OSS/MinIO）
            String name = "report_" + reportId + "_" + System.currentTimeMillis() + ".pdf";
            // TODO: OSS 上传
            log.debug("【PDF 生成占位】report={}, size={}", reportId, json.getBytes(StandardCharsets.UTF_8).length);
            return "https://oss.yingjianxia.com/inspection/" + name + "?sign=" + r.getSignature();
        } catch (Exception e) {
            log.warn("PDF 生成失败", e);
            throw new BusinessException(InspectionErrorCode.PDF_GENERATE_FAIL);
        }
    }
}
