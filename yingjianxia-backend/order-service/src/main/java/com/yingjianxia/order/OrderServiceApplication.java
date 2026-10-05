package com.yingjianxia.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.order",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.order.mapper")
@EnableScheduling
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 订单服务 (order-service) 启动成功
                   HTTP 端口: 8084
                   特性: 购物车 / TCC库存预扣 / 状态机流转 / 自动取消确认
                =========================================================
                """);
    }
}
