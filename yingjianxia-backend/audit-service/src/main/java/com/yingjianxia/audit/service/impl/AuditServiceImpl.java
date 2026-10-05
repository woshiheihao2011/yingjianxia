package com.yingjianxia.audit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.audit.dto.AuditQueryReq;
import com.yingjianxia.audit.dto.OperationLogReq;
import com.yingjianxia.audit.dto.ProcessAuditReq;
import com.yingjianxia.audit.entity.AuditRecord;
import com.yingjianxia.audit.entity.OperationLog;
import com.yingjianxia.audit.enums.AuditErrorCode;
import com.yingjianxia.audit.feign.ProductFeignClient;
import com.yingjianxia.audit.mapper.AuditRecordMapper;
import com.yingjianxia.audit.mapper.OperationLogMapper;
import com.yingjianxia.audit.service.AuditService;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核服务实现 — 商品审核流程（机审+人审） + 操作审计日志
 * <p>
 * 提交审核：商品提交 → 分配审核员 → 机审占位 → 待人审
 * 审核处理：通过 → 通知 product-service 上架；拒绝 → 通知卖家修改
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditRecordMapper auditRecordMapper;
    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;
    private final ProductFeignClient productFeignClient;

    /** 内部调用密钥（通过配置注入，生产环境务必修改） */
    @Value("${yingjianxia.internal-secret}") private String internalSecret;

    /** 机审通过阈值（占位） */
    private static final int MACHINE_PASS_THRESHOLD = 80;

    /* ======================== 提交审核 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitAudit(Integer targetType, Long targetId) {
        validateTargetType(targetType);
        // 防重：同一目标已存在待审核记录
        Long cnt = auditRecordMapper.selectCount(new LambdaQueryWrapper<AuditRecord>()
                .eq(AuditRecord::getTargetType, targetType)
                .eq(AuditRecord::getTargetId, targetId)
                .eq(AuditRecord::getStatus, AuditRecord.STATUS_PENDING));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(AuditErrorCode.AUDIT_TARGET_DUPLICATE);
        }

        // 分配审核员占位 — 生产环境按审核员负载均衡分配
        Long auditorId = assignAuditor();

        AuditRecord record = new AuditRecord();
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setAuditorId(auditorId);
        record.setAuditorName("审核员" + auditorId);
        record.setAction(AuditRecord.ACTION_PENDING);
        record.setStatus(AuditRecord.STATUS_PENDING);
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        auditRecordMapper.insert(record);

        // 机审占位：低风险直接通过，高风险转人审
        int machineScore = machineAudit(targetType, targetId);
        if (machineScore >= MACHINE_PASS_THRESHOLD) {
            log.info("【机审通过】recordId={}, score={}, 自动通过", record.getId(), machineScore);
            // 机审通过的记录仍需人审确认，保持 STATUS_PENDING
        } else {
            log.info("【机审存疑】recordId={}, score={}, 转人工审核", record.getId(), machineScore);
        }
        log.info("【提交审核】targetType={}, targetId={}, recordId={}, auditorId={}",
                targetType, targetId, record.getId(), auditorId);
        return record.getId();
    }

    /* ======================== 审核处理 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditRecord processAudit(Long recordId, ProcessAuditReq req, Long auditorId, String auditorName) {
        AuditRecord record = requireRecord(recordId);
        // 权限校验：分配的审核员才能处理
        if (record.getAuditorId() == null || !record.getAuditorId().equals(auditorId)) {
            throw new BusinessException(AuditErrorCode.NO_AUDIT_PERMISSION);
        }
        if (record.getStatus() != AuditRecord.STATUS_PENDING) {
            throw new BusinessException(AuditErrorCode.AUDIT_ALREADY_PROCESSED);
        }
        validateAction(req.getAction());

        Integer finalStatus;
        if (req.getAction() == AuditRecord.ACTION_PASS) {
            finalStatus = AuditRecord.STATUS_PASSED;
        } else {
            finalStatus = AuditRecord.STATUS_REJECTED;
        }
        auditRecordMapper.update(null, new LambdaUpdateWrapper<AuditRecord>()
                .eq(AuditRecord::getId, recordId)
                .set(AuditRecord::getAuditorId, auditorId)
                .set(AuditRecord::getAuditorName, auditorName)
                .set(AuditRecord::getAction, req.getAction())
                .set(AuditRecord::getReason, req.getReason())
                .set(AuditRecord::getComment, req.getComment())
                .set(AuditRecord::getEvidence, toJson(req.getEvidence()))
                .set(AuditRecord::getStatus, finalStatus)
                .set(AuditRecord::getUpdatedAt, LocalDateTime.now()));

        // 通知下游：通过→product-service 上架；拒绝→通知卖家修改
        notifyDownstream(record.getTargetType(), record.getTargetId(), req.getAction(), auditorId);

        // 记录操作日志
        recordOperationLog(buildLog(auditorId, 3, "AUDIT_PROCESS", "audit",
                record.getTargetType(), record.getTargetId(),
                toJsonString("action=" + req.getAction() + ",reason=" + req.getReason())));

        log.info("【审核处理】recordId={}, action={}, targetType={}, targetId={}",
                recordId, req.getAction(), record.getTargetType(), record.getTargetId());
        return auditRecordMapper.selectById(recordId);
    }

    /* ======================== 审核记录查询 ======================== */

    @Override
    public PageResult<AuditRecord> listAuditRecords(AuditQueryReq req) {
        LambdaQueryWrapper<AuditRecord> qw = new LambdaQueryWrapper<AuditRecord>()
                .eq(req.getTargetType() != null, AuditRecord::getTargetType, req.getTargetType())
                .eq(req.getTargetId() != null, AuditRecord::getTargetId, req.getTargetId())
                .eq(req.getStatus() != null, AuditRecord::getStatus, req.getStatus())
                .eq(req.getAuditorId() != null, AuditRecord::getAuditorId, req.getAuditorId())
                .orderByDesc(AuditRecord::getCreatedAt);
        IPage<AuditRecord> page = auditRecordMapper.selectPage(
                new Page<>(req.getPageNum(), req.getPageSize()), qw);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public AuditRecord getAuditRecord(Long recordId) {
        return requireRecord(recordId);
    }

    /* ======================== 操作日志 ======================== */

    @Override
    public void recordOperationLog(OperationLog log) {
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }
        operationLogMapper.insert(log);
    }

    @Override
    public PageResult<OperationLog> listOperationLogs(OperationLogReq req) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<OperationLog>()
                .eq(req.getUserId() != null, OperationLog::getUserId, req.getUserId())
                .eq(StrUtil.isNotBlank(req.getModule()), OperationLog::getModule, req.getModule())
                .eq(req.getTargetType() != null, OperationLog::getTargetType, req.getTargetType())
                .eq(req.getTargetId() != null, OperationLog::getTargetId, req.getTargetId())
                .orderByDesc(OperationLog::getCreatedAt);
        IPage<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(req.getPageNum(), req.getPageSize()), qw);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    /* ======================== 内部工具 ======================== */

    private AuditRecord requireRecord(Long id) {
        AuditRecord record = auditRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(AuditErrorCode.AUDIT_RECORD_NOT_FOUND);
        }
        return record;
    }

    private void validateTargetType(Integer targetType) {
        if (targetType == null || targetType < 1 || targetType > 4) {
            throw new BusinessException(AuditErrorCode.TARGET_TYPE_INVALID);
        }
    }

    private void validateAction(Integer action) {
        if (action == null || action < 1 || action > 3) {
            throw new BusinessException(AuditErrorCode.AUDIT_ACTION_INVALID);
        }
    }

    /** 分配审核员占位 — 生产环境按审核员负载均衡分配 */
    private Long assignAuditor() {
        // TODO: 对接审核员路由系统
        return 20001L;
    }

    /**
     * 机审占位 — 生产环境对接阿里云内容安全/自研模型
     * 返回 0~100 风险反向分（越高越安全）
     */
    private int machineAudit(Integer targetType, Long targetId) {
        // TODO: 对接内容安全 API，此处占位返回高分（低风险）
        return 90;
    }

    /**
     * 通知下游服务
     * 通过 → Feign 调用 product-service 商品上架（REVIEWING → ON_SALE）
     * 拒绝 → Feign 调用 product-service 商品状态变为审核拒绝
     */
    private void notifyDownstream(Integer targetType, Long targetId, Integer action, Long auditorId) {
        // 仅 targetType=1（商品审核）时通知 product-service
        if (targetType == null || targetType != 1) {
            log.debug("【跳过下游通知】非商品审核 targetType={}, targetId={}", targetType, targetId);
            return;
        }
        try {
            if (action == AuditRecord.ACTION_PASS) {
                var resp = productFeignClient.auditPass(targetId, internalSecret, auditorId);
                if (resp != null && resp.isSuccess()) {
                    log.info("【审核通过-通知上架成功】productId={}, auditorId={}", targetId, auditorId);
                } else {
                    log.error("【审核通过-通知上架失败】productId={}, resp={}", targetId, resp);
                }
            } else if (action == AuditRecord.ACTION_REJECT) {
                var resp = productFeignClient.auditReject(targetId, internalSecret, auditorId);
                if (resp != null && resp.isSuccess()) {
                    log.info("【审核拒绝-通知成功】productId={}, auditorId={}", targetId, auditorId);
                } else {
                    log.error("【审核拒绝-通知失败】productId={}, resp={}", targetId, resp);
                }
            } else if (action == AuditRecord.ACTION_TAKEDOWN) {
                log.info("【下架通知】targetId={}（待实现）", targetId);
            }
        } catch (Exception e) {
            log.error("【通知下游异常】targetType={}, targetId={}, action={}, err={}",
                    targetType, targetId, action, e.getMessage(), e);
        }
    }

    private OperationLog buildLog(Long userId, Integer userType, String action, String module,
                                 Integer targetType, Long targetId, String requestData) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserType(userType);
        log.setAction(action);
        log.setModule(module);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setRequestData(requestData);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将普通字符串包装为合法 JSON 字符串（用于 operation_logs.request_data 字段，该字段为 JSON 类型）
     */
    private String toJsonString(String text) {
        if (text == null) return "{}";
        try {
            return objectMapper.writeValueAsString(text);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
