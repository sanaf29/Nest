package com.nest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at {@code /swagger-ui.html}. Every operation requires the bearer scheme unless it
 * opts out (see {@code AuthController}). Use "Authorize" in the UI and paste the token from /login.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI nestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nest API")
                        .version("v1")
                        .description("Personal finance and budgeting API. Errors use RFC 7807 problem details."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
