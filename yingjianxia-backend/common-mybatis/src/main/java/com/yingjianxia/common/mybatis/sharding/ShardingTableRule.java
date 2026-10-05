package com.yingjianxia.common.mybatis.sharding;

import lombok.Getter;

/**
 * ShardingSphere 水平分表策略定义
 * <p>
 * 设计文档约定：5 张高增长表需要分表，此处统一管理配置避免散落。
 * </p>
 * <pre>
 * | 逻辑表名       | 分片键          | 分片数 | 路由策略       |
 * |----------------|-----------------|--------|----------------|
 * | orders         | buyer_id        | 8      | 取模哈希       |
 * | transactions   | user_id         | 8      | 取模哈希       |
 * | messages       | conversation_id | 16     | 取模哈希       |
 * | logistics_tracks | shipment_id  | 4      | 取模哈希       |
 * | points_records | user_id         | 4      | 取模哈希       |
 * </pre>
 *
 * @author 硬件侠后端团队
 */
@Getter
public enum ShardingTableRule {

    ORDERS("orders", "buyer_id", 8, HashModShardingAlgorithm.class),
    TRANSACTIONS("transactions", "user_id", 8, HashModShardingAlgorithm.class),
    MESSAGES("messages", "conversation_id", 16, HashModShardingAlgorithm.class),
    LOGISTICS_TRACKS("logistics_tracks", "shipment_id", 4, HashModShardingAlgorithm.class),
    POINTS_RECORDS("points_records", "user_id", 4, HashModShardingAlgorithm.class);

    private final String logicTable;
    private final String shardingColumn;
    private final int tableCount;
    private final Class<?> algorithmClass;

    ShardingTableRule(String logicTable, String shardingColumn, int tableCount, Class<?> algorithmClass) {
        this.logicTable = logicTable;
        this.shardingColumn = shardingColumn;
        this.tableCount = tableCount;
        this.algorithmClass = algorithmClass;
    }

    /**
     * 生成真实表表达式：orders_0..orders_7
     */
    public String actualDataNodes(String schema) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableCount; i++) {
            if (i > 0) sb.append(',');
            sb.append(schema).append('.').append(logicTable).append('_').append(i);
        }
        return sb.toString();
    }

    /**
     * 自定义分片算法类名（SPI）
     */
    public static class HashModShardingAlgorithm {
        // 分片值 -> hashCode -> Math.abs(hash) % tableCount
        public static int shard(Object value, int count) {
            int h;
            if (value == null) return 0;
            if (value instanceof Number num) {
                h = num.intValue();
            } else {
                h = value.toString().hashCode();
            }
            return Math.abs(h) % count;
        }
    }
}
