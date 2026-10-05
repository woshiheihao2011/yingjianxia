package com.yingjianxia.risk.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.risk.dto.RiskCheckReq;
import com.yingjianxia.risk.dto.RiskRuleCreateReq;
import com.yingjianxia.risk.dto.RiskScoreResp;
import com.yingjianxia.risk.entity.RiskRecord;
import com.yingjianxia.risk.entity.RiskRule;

/**
 * 风控服务接口
 *
 * @author 硬件侠后端团队
 */
public interface RiskService {

    /** 风控检查（频率检查/金额检查/行为分析 → 返回风险等级和处置建议） */
    RiskCheckResult checkRisk(RiskCheckReq req);

    /** 记录风控事件（内部） */
    Long recordRiskEvent(Long userId, String riskType, Integer riskLevel, Long ruleId,
                         String description, Integer action, String ipAddress, String deviceId);

    /* ========== 规则管理 ========== */

    /** 规则列表 */
    PageResult<RiskRule> listRules(Integer ruleType, Integer status, long pageNum, long pageSize);

    /** 新增/更新规则 */
    Long saveRule(RiskRuleCreateReq req);

    /** 删除规则 */
    void deleteRule(Long ruleId);

    /** 启用/停用规则 */
    void toggleRuleStatus(Long ruleId, Integer status);

    /* ========== 风险评分 ========== */

    /** 用户风险评分（根据历史记录计算） */
    RiskScoreResp getUserRiskScore(Long userId);

    /* ========== 风险记录查询 ========== */

    /** 风险记录查询 */
    PageResult<RiskRecord> listRiskRecords(Long userId, Integer riskLevel, long pageNum, long pageSize);

    /** 风控检查结果 */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class RiskCheckResult {
        /** 是否放行 */
        private boolean pass;
        /** 风险等级：1低 2中 3高 */
        private Integer riskLevel;
        /** 处置动作：1告警 2拦截 3验证 */
        private Integer action;
        /** 命中规则ID */
        private Long ruleId;
        /** 风险描述 */
        private String description;
    }
}
