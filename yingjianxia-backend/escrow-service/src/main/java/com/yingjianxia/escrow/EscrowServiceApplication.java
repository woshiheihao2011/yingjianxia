package com.yingjianxia.escrow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 担保资金服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.escrow",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.escrow.mapper")
@EnableScheduling
public class EscrowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EscrowServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 担保资金服务 (escrow-service) 启动成功
                   HTTP 端口: 8085
                   特性: 钱包管理 / 担保冻结放款 / 乐观锁并发 / T+1提现 / 全链路流水
                =========================================================
                """);
    }
}
