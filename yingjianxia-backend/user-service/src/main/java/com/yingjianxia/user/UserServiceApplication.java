package com.yingjianxia.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户服务启动类
 * <p>
 * 负责：用户注册登录/JWT/信用分/实名认证/收货地址/店铺/评价
 * </p>
 *
 * @author 硬件侠后端团队
 */
@SpringBootApplication(scanBasePackages = {
        "com.yingjianxia.user",
        "com.yingjianxia.common.core",
        "com.yingjianxia.common.web",
        "com.yingjianxia.common.feign",
        "com.yingjianxia.common.mybatis",
        "com.yingjianxia.common.mq"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yingjianxia")
@MapperScan("com.yingjianxia.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("""
                =========================================================
                 🚀 硬件侠平台 — 用户服务 (user-service) 启动成功
                   HTTP 端口: 8081 (默认)
                   Knife4j:  http://localhost:8081/doc.html
                   Nacos:    已注册为 user-service
                   XXL-Job:  userCreditScoreRefreshJob
                =========================================================
                """);
    }
}
