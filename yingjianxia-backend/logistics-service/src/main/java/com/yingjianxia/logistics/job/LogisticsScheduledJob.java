package com.yingjianxia.logistics.job;

import com.yingjianxia.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 物流定时任务 — 超时自动签收
 * <p>
 * 每天 02:00 扫描发货超过 15 天未签收的记录，自动置为已签收。
 * 生产环境可替换为 XXL-Job 分布式调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticsScheduledJob {

    private final LogisticsService logisticsService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void autoSignReceived() {
        log.info("[定时任务] 开始执行超时自动签收...");
        try {
            logisticsService.autoSignReceived();
        } catch (Exception e) {
            log.error("[定时任务] 超时自动签收执行失败", e);
        }
    }
}
