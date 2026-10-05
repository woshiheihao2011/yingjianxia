package com.yingjianxia.logistics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.logistics",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.logistics.mapper")
@EnableScheduling
public class LogisticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogisticsServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 物流服务 (logistics-service) 启动成功
                   HTTP 端口: 8087
                   特性: 发货管理 / 物流轨迹回调 / 自动签收
                =========================================================
                """);
    }
}
