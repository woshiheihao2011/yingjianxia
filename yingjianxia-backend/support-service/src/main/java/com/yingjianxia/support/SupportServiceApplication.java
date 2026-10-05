package com.yingjianxia.support;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.support",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@EnableScheduling
@MapperScan("com.yingjianxia.support.mapper")
public class SupportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 客服支撑服务 (support-service) 启动成功
                   HTTP 端口: 8093
                   特性: 工单系统 / 快捷回复 / FAQ / 公告 / 举报处理
                =========================================================
                """);
    }
}
