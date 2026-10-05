package com.yingjianxia.audit.service;

import com.yingjianxia.audit.dto.AuditQueryReq;
import com.yingjianxia.audit.dto.OperationLogReq;
import com.yingjianxia.audit.dto.ProcessAuditReq;
import com.yingjianxia.audit.entity.AuditRecord;
import com.yingjianxia.audit.entity.OperationLog;
import com.yingjianxia.common.core.result.PageResult;

/**
 * 审核服务接口
 *
 * @author 硬件侠后端团队
 */
public interface AuditService {

    /** 提交审核（商品提交审核 → 分配审核员 → 机审+人审流程占位） */
    Long submitAudit(Integer targetType, Long targetId);

    /** 审核处理（通过→通知 product-service 上架；拒绝→通知卖家修改） */
    AuditRecord processAudit(Long recordId, ProcessAuditReq req, Long auditorId, String auditorName);

    /** 审核记录查询 */
    PageResult<AuditRecord> listAuditRecords(AuditQueryReq req);

    /** 审核记录详情 */
    AuditRecord getAuditRecord(Long recordId);

    /* ========== 操作日志 ========== */

    /** 记录操作日志（内部调用） */
    void recordOperationLog(OperationLog log);

    /** 操作日志查询 */
    PageResult<OperationLog> listOperationLogs(OperationLogReq req);
}
