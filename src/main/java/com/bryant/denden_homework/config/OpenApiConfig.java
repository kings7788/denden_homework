package com.bryant.denden_homework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI metadata. Swagger UI is served at /swagger-ui.html and
 * the spec at /v3/api-docs (both permitted in SecurityConfig).
 *
 * Declares a "bearerAuth" scheme so the Swagger "Authorize" button lets you paste
 * the JWT returned by POST /api/auth/login/verify and call protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI memberSystemOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Member System API")
                        .version("v1")
                        .description("""
                                會員系統 API:
                                1. 註冊(Email 為帳號)→ 寄開通信
                                2. 開通帳號(點開通連結)
                                3. 登入(帳密)→ 寄 Email OTP → 驗證 OTP 取得 JWT(兩階段認證)
                                4. 查詢自己的最後登入時間(需 JWT,非本人無法查詢)

                                用法:先用 /api/auth/login + /api/auth/login/verify 取得 access_token,
                                點右上 Authorize 貼上後即可呼叫受保護的 /api/users 端點。"""))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access_token from /api/auth/login/verify")));
    }
}
