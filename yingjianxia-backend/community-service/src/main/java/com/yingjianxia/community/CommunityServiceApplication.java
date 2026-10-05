package com.yingjianxia.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.community",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@EnableScheduling
@MapperScan("com.yingjianxia.community.mapper")
public class CommunityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 社区服务 (community-service) 启动成功
                   HTTP 端口: 8090
                   特性: 帖子CRUD / 评论回复 / 点赞 / 关注 / 浏览量Redis计数
                =========================================================
                """);
    }
}
