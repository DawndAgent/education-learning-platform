package com.xxedu.learning.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "Bearer";

    @Bean
    public OpenAPI learningOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("XX教育学习中心 API")
                        .description("模块化单体。Sprint 01 仅包含基础设施接口，业务模块尚未开放。")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("AccessToken")
                        .description("管理端访问令牌。登录签发在后续账号模块实现。")));
    }
}
