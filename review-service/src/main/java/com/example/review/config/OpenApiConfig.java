package com.example.review.config;

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
    public OpenAPI reviewOpenAPI() {
        String bearerScheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant Review Service API")
                        .description("리뷰 조회, 작성, 수정, 삭제 API 명세")
                        .version("v1"))
                .addServersItem(new Server().url("http://localhost:8084").description("Review Service"))
                .addServersItem(new Server().url("http://localhost:8080").description("API Gateway"))
                .components(new Components().addSecuritySchemes(bearerScheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme));
    }
}
