package com.yingjianxia.common.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j (OpenAPI 3.0) 全局文档配置
 * <p>
 * 统一文档标题、版本、鉴权方式（Bearer JWT）、公共 Header（幂等键/TraceId）。
 * 访问地址：{@code http://{host}:{port}/doc.html}
 *
 * @author 硬件侠后端团队
 */
@Configuration
public class Knife4jConfig {

    /**
     * Swagger Bearer Security scheme 名称
     */
    public static final String SCHEME_NAME = "Authorization";
    public static final String SCHEME_IDEMPOTENT = "IdempotencyKey";

    @Bean
    public OpenAPI yingjianxiaOpenAPI() {
        Server devServer = new Server().url("http://localhost:8080").description("开发环境网关");
        Server testServer = new Server().url("https://test-api.yingjianxia.com").description("测试环境");

        Contact contact = new Contact()
                .name("硬件侠后端团队")
                .email("backend@yingjianxia.com");

        License license = new License()
                .name("Copyright © 2026 硬件侠 Technology")
                .url("https://www.yingjianxia.com");

        Info info = new Info()
                .title("硬件侠平台 — 开放 API 文档")
                .description(
                        "## 硬件侠（C2C 电脑配件二手交易平台）\n\n" +
                        "### 一、鉴权说明  \n" +
                        "- 注册登录后获取 `access_token`，在请求头中携带：`Authorization: Bearer {token}`\n" +
                        "- Token 过期时间 2h，可使用 refresh_token（7天）换发新 token\n" +
                        "### 二、幂等说明  \n" +
                        "- 所有写请求（POST/PUT/DELETE）建议在 Header 中传 `X-Idempotency-Key: {UUID}`\n" +
                        "- 同幂等键 60s 内重复请求会被拒绝\n" +
                        "### 三、响应结构  \n" +
                        "统一返回：`{ code, message, data, timestamp, traceId }`，code=0 表示成功。"
                )
                .version("1.0.0")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, testServer))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SCHEME_NAME)
                        .addList(SCHEME_IDEMPOTENT))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("请输入 Access Token，格式：Bearer {token}"))
                        .addSecuritySchemes(SCHEME_IDEMPOTENT,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Idempotency-Key")
                                        .description("写请求幂等键，建议使用 UUID。同 Key 60s 内仅能请求一次。")));
    }
}
