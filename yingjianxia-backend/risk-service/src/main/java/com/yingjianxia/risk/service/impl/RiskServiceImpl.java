package com.yingjianxia.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.risk.dto.RiskCheckReq;
import com.yingjianxia.risk.dto.RiskRuleCreateReq;
import com.yingjianxia.risk.dto.RiskScoreResp;
import com.yingjianxia.risk.entity.RiskRecord;
import com.yingjianxia.risk.entity.RiskRule;
import com.yingjianxia.risk.entity.UserRiskScore;
import com.yingjianxia.risk.enums.RiskErrorCode;
import com.yingjianxia.risk.mapper.RiskRecordMapper;
import com.yingjianxia.risk.mapper.RiskRuleMapper;
import com.yingjianxia.risk.mapper.UserRiskScoreMapper;
import com.yingjianxia.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 风控服务实现 — 交易风险识别和拦截
 * <p>
 * 风控检查：频率检查（Redis 滑动计数）/ 金额检查 / 行为分析 → 返回风险等级和处置建议
 * 规则管理：CRUD
 * 风险评分：根据历史风控记录计算用户风险分
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RiskRuleMapper ruleMapper;
    private final RiskRecordMapper recordMapper;
    private final UserRiskScoreMapper scoreMapper;
    private final StringRedisTemplate redisTemplate;

    /** 频率检查 Redis Key：yjx:risk:freq:{userId}:{action} */
    private static final String FREQ_KEY = "yjx:risk:freq:%s:%s";
    /** 频率检查窗口（秒） */
    private static final long FREQ_WINDOW_SECONDS = 60L;
    /** 频率阈值（60秒内超过则告警） */
    private static final int FREQ_THRESHOLD = 10;
    /** 金额阈值（超过则拦截） */
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("50000");

    /* ======================== 风控检查 ======================== */

    @Override
    public RiskCheckResult checkRisk(RiskCheckReq req) {
        // 1. 高风险用户直接拦截
        UserRiskScore score = scoreMapper.selectOne(new LambdaQueryWrapper<UserRiskScore>()
                .eq(UserRiskScore::getUserId, req.getUserId()));
        if (score != null && score.getLevel() == UserRiskScore.LEVEL_HIGH) {
            log.warn("【风控拦截】高风险用户 userId={}, score={}", req.getUserId(), score.getScore());
            recordRiskEvent(req.getUserId(), "高风险用户", RiskRecord.LEVEL_HIGH, null,
                    "用户风险评分过高", RiskRecord.ACTION_BLOCK, req.getIpAddress(), req.getDeviceId());
            throw new BusinessException(RiskErrorCode.HIGH_RISK_USER);
        }

        // 2. 频率检查
        int freq = checkFrequency(req.getUserId(), req.getAction());
        if (freq > FREQ_THRESHOLD) {
            RiskCheckResult result = new RiskCheckResult();
            result.setPass(false);
            result.setRiskLevel(RiskRecord.LEVEL_MEDIUM);
            result.setAction(RiskRecord.ACTION_VERIFY);
            result.setDescription("操作过于频繁，请稍后再试");
            recordRiskEvent(req.getUserId(), "频率异常", RiskRecord.LEVEL_MEDIUM, null,
                    "60秒内操作" + freq + "次", RiskRecord.ACTION_VERIFY, req.getIpAddress(), req.getDeviceId());
            return result;
        }

        // 3. 金额检查
        if (req.getAmount() != null && req.getAmount().compareTo(AMOUNT_THRESHOLD) > 0) {
            RiskCheckResult result = new RiskCheckResult();
            result.setPass(false);
            result.setRiskLevel(RiskRecord.LEVEL_HIGH);
            result.setAction(RiskRecord.ACTION_BLOCK);
            result.setDescription("交易金额超过阈值，已拦截");
            recordRiskEvent(req.getUserId(), "金额异常", RiskRecord.LEVEL_HIGH, null,
                    "交易金额=" + req.getAmount() + " 超过阈值", RiskRecord.ACTION_BLOCK,
                    req.getIpAddress(), req.getDeviceId());
            return result;
        }

        // 4. 行为分析占位 — 生产环境对接规则引擎 Drools / ML 模型
        // 命中启用的规则
        List<RiskRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getStatus, RiskRule.STATUS_ENABLED));
        for (RiskRule rule : rules) {
            if (matchRule(rule, req)) {
                RiskCheckResult result = new RiskCheckResult();
                result.setPass(rule.getAction() != RiskRule.ACTION_BLOCK);
                result.setRiskLevel(RiskRecord.LEVEL_MEDIUM);
                result.setAction(rule.getAction());
                result.setRuleId(rule.getId());
                result.setDescription("命中规则：" + rule.getRuleName());
                recordRiskEvent(req.getUserId(), rule.getRuleName(), RiskRecord.LEVEL_MEDIUM,
                        rule.getId(), "命中规则：" + rule.getRuleName(), rule.getAction(),
                        req.getIpAddress(), req.getDeviceId());
                return result;
            }
        }

        // 5. 放行
        RiskCheckResult result = new RiskCheckResult();
        result.setPass(true);
        result.setRiskLevel(RiskRecord.LEVEL_LOW);
        result.setAction(RiskRule.ACTION_WARN);
        result.setDescription("放行");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordRiskEvent(Long userId, String riskType, Integer riskLevel, Long ruleId,
                                String description, Integer action, String ipAddress, String deviceId) {
        RiskRecord record = new RiskRecord();
        record.setUserId(userId);
        record.setRiskType(riskType);
        record.setRiskLevel(riskLevel);
        record.setRuleId(ruleId);
        record.setDescription(description);
        record.setAction(action);
        record.setIpAddress(ipAddress);
        record.setDeviceId(deviceId);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        // 触发风险评分重算
        recalculateScore(userId);
        log.info("【记录风控事件】userId={}, riskType={}, level={}, action={}", userId, riskType, riskLevel, action);
        return record.getId();
    }

    /* ======================== 规则管理 ======================== */

    @Override
    public PageResult<RiskRule> listRules(Integer ruleType, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<RiskRule> qw = new LambdaQueryWrapper<RiskRule>()
                .eq(ruleType != null, RiskRule::getRuleType, ruleType)
                .eq(status != null, RiskRule::getStatus, status)
                .orderByDesc(RiskRule::getCreatedAt);
        IPage<RiskRule> page = ruleMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRule(RiskRuleCreateReq req) {
        validateRuleType(req.getRuleType());
        validateAction(req.getAction());
        LocalDateTime now = LocalDateTime.now();
        RiskRule rule;
        if (req.getId() == null) {
            // 防重：同名规则
            Long cnt = ruleMapper.selectCount(new LambdaQueryWrapper<RiskRule>()
                    .eq(RiskRule::getRuleName, req.getRuleName()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException(RiskErrorCode.RULE_DUPLICATE);
            }
            rule = new RiskRule();
            rule.setRuleName(req.getRuleName());
            rule.setRuleType(req.getRuleType());
            rule.setRuleConfig(req.getRuleConfig());
            rule.setAction(req.getAction());
            rule.setStatus(req.getStatus() == null ? RiskRule.STATUS_ENABLED : req.getStatus());
            rule.setCreatedAt(now);
            rule.setUpdatedAt(now);
            ruleMapper.insert(rule);
        } else {
            rule = ruleMapper.selectById(req.getId());
            if (rule == null) {
                throw new BusinessException(RiskErrorCode.RULE_NOT_FOUND);
            }
            ruleMapper.update(null, new LambdaUpdateWrapper<RiskRule>()
                    .eq(RiskRule::getId, req.getId())
                    .set(RiskRule::getRuleName, req.getRuleName())
                    .set(RiskRule::getRuleType, req.getRuleType())
                    .set(RiskRule::getRuleConfig, req.getRuleConfig())
                    .set(RiskRule::getAction, req.getAction())
                    .set(RiskRule::getStatus, req.getStatus() == null ? rule.getStatus() : req.getStatus())
                    .set(RiskRule::getUpdatedAt, now));
        }
        return rule.getId() == null ? req.getId() : rule.getId();
    }

    @Override
    public void deleteRule(Long ruleId) {
        if (ruleMapper.selectById(ruleId) == null) {
            throw new BusinessException(RiskErrorCode.RULE_NOT_FOUND);
        }
        ruleMapper.deleteById(ruleId);
    }

    @Override
    public void toggleRuleStatus(Long ruleId, Integer status) {
        if (ruleMapper.selectById(ruleId) == null) {
            throw new BusinessException(RiskErrorCode.RULE_NOT_FOUND);
        }
        ruleMapper.update(null, new LambdaUpdateWrapper<RiskRule>()
                .eq(RiskRule::getId, ruleId)
                .set(RiskRule::getStatus, status)
                .set(RiskRule::getUpdatedAt, LocalDateTime.now()));
    }

    /* ======================== 风险评分 ======================== */

    @Override
    public RiskScoreResp getUserRiskScore(Long userId) {
        UserRiskScore score = scoreMapper.selectOne(new LambdaQueryWrapper<UserRiskScore>()
                .eq(UserRiskScore::getUserId, userId));
        if (score == null) {
            // 无记录视为低风险
            RiskScoreResp resp = new RiskScoreResp();
            resp.setUserId(userId);
            resp.setScore(0);
            resp.setLevel(UserRiskScore.LEVEL_LOW);
            resp.setLevelDesc("低风险");
            return resp;
        }
        RiskScoreResp resp = new RiskScoreResp();
        resp.setUserId(userId);
        resp.setScore(score.getScore());
        resp.setLevel(score.getLevel());
        resp.setLevelDesc(switch (score.getLevel()) {
            case UserRiskScore.LEVEL_HIGH -> "高风险";
            case UserRiskScore.LEVEL_MEDIUM -> "中风险";
            default -> "低风险";
        });
        return resp;
    }

    /* ======================== 风险记录查询 ======================== */

    @Override
    public PageResult<RiskRecord> listRiskRecords(Long userId, Integer riskLevel, long pageNum, long pageSize) {
        LambdaQueryWrapper<RiskRecord> qw = new LambdaQueryWrapper<RiskRecord>()
                .eq(userId != null, RiskRecord::getUserId, userId)
                .eq(riskLevel != null, RiskRecord::getRiskLevel, riskLevel)
                .orderByDesc(RiskRecord::getCreatedAt);
        IPage<RiskRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(pageNum, pageSize, page.getTotal(), page.getRecords());
    }

    /* ======================== 内部工具 ======================== */

    /** 频率检查：滑动窗口内操作计数（Redis INCR + TTL） */
    private int checkFrequency(Long userId, String action) {
        String key = String.format(FREQ_KEY, userId, action);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, FREQ_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        return count == null ? 0 : count.intValue();
    }

    /**
     * 规则匹配占位 — 生产环境对接 Drools 规则引擎解析 ruleConfig JSON
     */
    private boolean matchRule(RiskRule rule, RiskCheckReq req) {
        // TODO: 解析 ruleConfig JSON 并按规则类型匹配
        return false;
    }

    /**
     * 根据历史风控记录重算用户风险评分
     * 评分逻辑：近 30 天风控记录数 × 权重 + 高风险记录加权
     */
    @Transactional(rollbackFor = Exception.class)
    public void recalculateScore(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Long totalCount = recordMapper.selectCount(new LambdaQueryWrapper<RiskRecord>()
                .eq(RiskRecord::getUserId, userId)
                .ge(RiskRecord::getCreatedAt, since));
        Long highCount = recordMapper.selectCount(new LambdaQueryWrapper<RiskRecord>()
                .eq(RiskRecord::getUserId, userId)
                .eq(RiskRecord::getRiskLevel, RiskRecord.LEVEL_HIGH)
                .ge(RiskRecord::getCreatedAt, since));
        long total = totalCount == null ? 0 : totalCount;
        long high = highCount == null ? 0 : highCount;
        // 评分：每条记录 +5，高风险记录额外 +15，上限 100
        int score = (int) Math.min(100, total * 5 + high * 15);
        int level;
        if (score >= UserRiskScore.THRESHOLD_HIGH) {
            level = UserRiskScore.LEVEL_HIGH;
        } else if (score >= UserRiskScore.THRESHOLD_MEDIUM) {
            level = UserRiskScore.LEVEL_MEDIUM;
        } else {
            level = UserRiskScore.LEVEL_LOW;
        }

        UserRiskScore existing = scoreMapper.selectOne(new LambdaQueryWrapper<UserRiskScore>()
                .eq(UserRiskScore::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            UserRiskScore s = new UserRiskScore();
            s.setUserId(userId);
            s.setScore(score);
            s.setLevel(level);
            s.setLastUpdate(now);
            s.setCreatedAt(now);
            s.setUpdatedAt(now);
            scoreMapper.insert(s);
        } else {
            scoreMapper.update(null, new LambdaUpdateWrapper<UserRiskScore>()
                    .eq(UserRiskScore::getId, existing.getId())
                    .set(UserRiskScore::getScore, score)
                    .set(UserRiskScore::getLevel, level)
                    .set(UserRiskScore::getLastUpdate, now)
                    .set(UserRiskScore::getUpdatedAt, now));
        }
    }

    private void validateRuleType(Integer ruleType) {
        if (ruleType == null || ruleType < 1 || ruleType > 4) {
            throw new BusinessException(RiskErrorCode.RULE_TYPE_INVALID);
        }
    }

    private void validateAction(Integer action) {
        if (action == null || action < 1 || action > 3) {
            throw new BusinessException(RiskErrorCode.RULE_ACTION_INVALID);
        }
    }
}
