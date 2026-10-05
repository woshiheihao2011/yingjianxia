package com.yingjianxia.marketing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 营销服务启动类
 * <p>
 * 职责：优惠券、营销活动、积分体系。
 * 安全核心：优惠券防超发（原子扣减）、积分账户乐观锁。
 */
@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.marketing",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.marketing.mapper")
@EnableScheduling
public class MarketingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketingServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🎫 硬件侠平台 — 营销服务 (marketing-service) 启动成功
                   HTTP 端口: 8091
                   特性: 优惠券(防超发) / 积分(乐观锁) / 营销活动 / 签到
                =========================================================
                """);
    }
}
