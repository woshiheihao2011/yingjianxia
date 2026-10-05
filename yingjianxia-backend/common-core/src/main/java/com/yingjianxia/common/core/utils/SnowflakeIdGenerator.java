package com.yingjianxia.common.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 雪花算法 ID 生成器（自研精简版，避免引入额外依赖）
 * <p>
 * 结构： 1 位符号位 + 41 位时间戳 + 10 位机器号 + 12 位序列号
 * <br>
 * 支持单节点 4096 ids/ms 并发，理论可用至 2088 年。
 * <p>
 * 机器号获取优先级：{@code yingjianxia.snowflake.worker-id} 配置 → MAC 哈希 → SecureRandom
 *
 * @author 硬件侠后端团队
 */
@Slf4j
public class SnowflakeIdGenerator {

    /* ========== 时间部分 ========== */
    /** 起始时间戳：2024-01-01 00:00:00 */
    private static final long EPOCH = 1704038400000L;
    private static final long TIMESTAMP_BITS = 41L;
    private static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BITS) - 1;

    /* ========== 机器号 ========== */
    private static final long WORKER_BITS = 10L;
    private static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1;

    /* ========== 序列号 ========== */
    private static final long SEQUENCE_BITS = 12L;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    /* ========== 偏移量 ========== */
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = WORKER_BITS + SEQUENCE_BITS;

    /* ========== 实例 ========== */
    private final long workerId;
    private final AtomicInteger sequence = new AtomicInteger(0);
    private volatile long lastTimestamp = -1L;

    private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator();

    private SnowflakeIdGenerator() {
        long configured = -1L;
        String envWorkerId = System.getenv("YINGJIANXIA_WORKER_ID");
        String propWorkerId = System.getProperty("yingjianxia.snowflake.worker-id");
        if (propWorkerId != null && !propWorkerId.isBlank()) {
            try { configured = Long.parseLong(propWorkerId); } catch (Exception ignored) {}
        }
        if (configured < 0 && envWorkerId != null && !envWorkerId.isBlank()) {
            try { configured = Long.parseLong(envWorkerId); } catch (Exception ignored) {}
        }
        if (configured < 0) {
            configured = computeWorkerIdFromMac() & MAX_WORKER_ID;
        }
        this.workerId = configured & MAX_WORKER_ID;
        log.info("[Snowflake] 初始化 workerId = {}", this.workerId);
    }

    public static SnowflakeIdGenerator getInstance() { return INSTANCE; }

    /**
     * 生成全局唯一 ID
     */
    public synchronized long nextId() {
        long ts = currentTimeMillis();
        if (ts < lastTimestamp) {
            long diff = lastTimestamp - ts;
            if (diff <= 5) {
                try { wait(diff << 1); ts = currentTimeMillis(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            if (ts < lastTimestamp) {
                throw new RuntimeException("时钟回拨超过容忍值，已停止生成ID：backward=" + (lastTimestamp - ts) + "ms");
            }
        }
        if (ts == lastTimestamp) {
            int seq = sequence.incrementAndGet();
            if ((seq & SEQUENCE_MASK) == 0) {
                // 序号溢出，等下一毫秒
                ts = tilNextMillis(lastTimestamp);
                sequence.set(0);
            }
        } else {
            sequence.set(new SecureRandom().nextInt(10)); // 低并发随机起始，避免低位全 0
        }
        lastTimestamp = ts;
        long elapsed = ts - EPOCH;
        if (elapsed > MAX_TIMESTAMP) {
            throw new RuntimeException("时间戳位已用尽，雪花算法已到使用年限！");
        }
        return (elapsed << TIMESTAMP_SHIFT)
                | (workerId << WORKER_SHIFT)
                | (sequence.get() & SEQUENCE_MASK);
    }

    /**
     * 返回 String 类型 ID（前端兼容 JS 53bit 安全整数）
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long currentTimeMillis() {
        return Instant.now().toEpochMilli();
    }

    private long tilNextMillis(long lastTs) {
        long ts = currentTimeMillis();
        while (ts <= lastTs) { ts = currentTimeMillis(); }
        return ts;
    }

    private static long computeWorkerIdFromMac() {
        try {
            Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
            StringBuilder sb = new StringBuilder();
            while (netIfs.hasMoreElements()) {
                NetworkInterface ni = netIfs.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && !ni.isLoopback()) {
                    for (byte b : mac) sb.append(String.format("%02X", b));
                    sb.append('|');
                }
            }
            if (sb.length() == 0) {
                SecureRandom r = new SecureRandom();
                return r.nextLong();
            }
            long h = 0x811c9dc5L;
            for (byte b : sb.toString().getBytes()) {
                h ^= (b & 0xFF);
                h *= 0x01000193L;
            }
            return h;
        } catch (Exception e) {
            log.warn("[Snowflake] MAC获取失败，改用 SecureRandom 生成 workerId", e);
            return new SecureRandom().nextLong();
        }
    }
}
