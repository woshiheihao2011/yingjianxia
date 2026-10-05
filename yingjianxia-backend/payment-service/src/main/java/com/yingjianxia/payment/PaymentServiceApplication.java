package com.yingjianxia.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 支付服务启动类
 * <p>
 * 职责：对接微信/支付宝第三方支付渠道，处理充值与支付回调。
 * 验签 + 幂等 + 主动查询兜底是三大资金安全基石。
 */
@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.payment",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.payment.mapper")
@EnableScheduling
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 💳 硬件侠平台 — 支付服务 (payment-service) 启动成功
                   HTTP 端口: 8086
                   特性: 微信/支付宝统一下单 / 回调验签+幂等 / 超时关单
                =========================================================
                """);
    }
}
