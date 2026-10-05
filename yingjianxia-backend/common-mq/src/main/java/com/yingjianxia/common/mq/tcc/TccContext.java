package com.yingjianxia.common.mq.tcc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * TCC 事务上下文 — 贯穿 Try / Confirm / Cancel 三阶段
 * <p>
 * 所有 TCC 动作必须携带此上下文，其中：
 * <ul>
 *   <li>{@code idempotentKey}  唯一幂等键（例：TCC:ESCROW:FREEZE:{orderId}）</li>
 *   <li>{@code xid}             Seata/自研全局事务ID（预留，若后续接入）</li>
 *   <li>{@code branchId}        分支ID</li>
 *   <li>{@code action}          当前阶段：TRY / CONFIRM / CANCEL</li>
 *   <li>{@code attachments}     业务扩展参数（Try 中计算后传给 Confirm/Cancel）</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TccContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 阶段枚举 */
    public enum Phase { TRY, CONFIRM, CANCEL }

    private String idempotentKey;
    private String xid;
    private String branchId;
    private Phase  action = Phase.TRY;

    /** 发起方 userId（用于审计） */
    private Long   operatorId;

    /** 业务扩展参数（Try 写入，Confirm/Cancel 可读取） */
    @Builder.Default
    private Map<String, Object> attachments = new HashMap<>();

    /* ---------- 便捷方法 ---------- */

    public TccContext put(String key, Object value) {
        if (attachments == null) attachments = new HashMap<>();
        attachments.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return attachments == null ? null : (T) attachments.get(key);
    }

    public static TccContext of(String idempotentKey, Long operatorId) {
        return TccContext.builder()
                .idempotentKey(idempotentKey)
                .operatorId(operatorId)
                .action(Phase.TRY)
                .build();
    }
}
