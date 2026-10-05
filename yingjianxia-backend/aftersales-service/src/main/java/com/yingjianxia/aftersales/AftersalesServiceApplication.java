package com.yingjianxia.aftersales;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.aftersales",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.aftersales.mapper")
@EnableScheduling
public class AftersalesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AftersalesServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 售后服务 (aftersales-service) 启动成功
                   HTTP 端口: 8088
                   特性: 退款/退货退款/换货 / 状态日志 / 仲裁 / 超时自动同意
                =========================================================
                """);
    }
}
