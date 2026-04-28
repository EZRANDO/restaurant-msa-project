package com.example.menu.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuOpenAPI() {
        String bearerScheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant Menu Service API")
                        .description("카테고리와 메뉴 등록, 수정, 삭제, 조회 API 명세")
                        .version("v1"))
                .addServersItem(new Server().url("http://localhost:8082").description("Menu Service"))
                .addServersItem(new Server().url("http://localhost:8080").description("API Gateway"))
                .components(new Components().addSecuritySchemes(bearerScheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme));
    }
}
