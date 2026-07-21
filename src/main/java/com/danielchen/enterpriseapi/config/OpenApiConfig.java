package com.danielchen.enterpriseapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String APIKEY_SCHEME = "apiKeyAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise API")
                        .version("v1")
                        .description("""
                                Multi-tenant REST API with role-based access control, \
                                per-tenant data isolation (PostgreSQL RLS), \
                                and distributed rate limiting (Bucket4j + Redis).

                                **Authentication:** Use `Bearer <JWT>` for human users \
                                or `ApiKey <key>` for machine clients.
                                """))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_SCHEME)
                        .addList(APIKEY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT issued by POST /api/v1/auth/login"))
                        .addSecuritySchemes(APIKEY_SCHEME, new SecurityScheme()
                                .name(APIKEY_SCHEME)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Format: ApiKey <key> — issued by POST /api/v1/apikeys")));
    }
}
