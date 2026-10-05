package com.yingjianxia.inspection;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.inspection",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.inspection.mapper")
public class InspectionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspectionServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 验机服务 (inspection-service) 启动成功
                   HTTP 端口: 8083
                   特性: 12项检测 / SHA-256签名验签 / PDF报告
                =========================================================
                """);
    }
}
