package com.yingjianxia.inspection.service;

import com.yingjianxia.inspection.dto.InspectionSubmitReq;
import com.yingjianxia.inspection.entity.InspectionReport;
import com.yingjianxia.inspection.entity.InspectionTemplate;

import java.util.List;

/**
 * 验机服务接口
 */
public interface InspectionService {

    /* ====== 模板 ====== */
    /** 查看某分类的12项检测模板（0=通用） */
    List<InspectionTemplate> listTemplates(Integer categoryId);

    /* ====== 创建 & 状态流转（检测员端 / 入库端） ====== */
    /** 为商品创建验机报告（卖家/平台发起），状态=待检测 */
    Long createReport(Long productId, Long orderId, Long inspectorId, String inspectorName);
    /** 开始检测 → 状态=检测中 */
    void startTesting(Long reportId, Long inspectorId);
    /** 提交12项结果 → 计算grade/SHA-256签名 → 生成PDF → 状态=已完成 → 发MQ通知商品域 上架 */
    InspectionReport submitResult(InspectionSubmitReq req, Long inspectorId);

    /* ====== 查询 & 校验 ====== */
    /** 报告详情（含12项） */
    InspectionReport getReportDetail(Long reportId);
    /** 根据商品ID查报告（详情页展示用） */
    InspectionReport getReportByProduct(Long productId);
    /** 验签：报告完整性校验（SHA-256） */
    boolean verifySignature(Long reportId);

    /* ====== 报告下载 ====== */
    /** 生成或查询 PDF 报告 URL */
    String generateOrGetPdfUrl(Long reportId);
}
