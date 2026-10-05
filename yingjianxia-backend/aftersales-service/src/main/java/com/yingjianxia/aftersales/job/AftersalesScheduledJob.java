package com.yingjianxia.aftersales.job;

import com.yingjianxia.aftersales.service.AfterSalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 售后定时任务 — 卖家超时未处理自动同意
 * <p>
 * 每小时扫描审核中且超过自动处理截止时间的售后单，系统自动同意。
 * 生产环境可替换为 XXL-Job 分布式调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AftersalesScheduledJob {

    private final AfterSalesService afterSalesService;

    @Scheduled(cron = "0 0 * * * ?")
    public void autoAgreeTimeout() {
        log.info("[定时任务] 开始执行售后超时自动同意...");
        try {
            afterSalesService.autoAgreeTimeout();
        } catch (Exception e) {
            log.error("[定时任务] 售后超时自动同意执行失败", e);
        }
    }
}
