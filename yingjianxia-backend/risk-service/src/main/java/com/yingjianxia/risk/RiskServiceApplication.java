package com.yingjianxia.risk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.risk",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.risk.mapper")
public class RiskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 风控服务 (risk-service) 启动成功
                   HTTP 端口: 8094
                   特性: 风控检查 / 规则引擎 / 用户风险评分 / 风险记录
                =========================================================
                """);
    }
}
