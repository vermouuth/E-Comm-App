package com.ecomm.sb_ecomm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String COOKIE_AUTH = "cookieAuth";

    @Value("${spring.app.ecom.jwtCookeName}")
    private String jwtCookieName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Comm API")
                        .version("v1")
                        .description("""
                                REST API for the E-Comm platform: authentication, addresses, categories, \
                                products, cart, orders and payments.

                                Authentication uses a JWT delivered as an HTTP cookie named "%s" (not a \
                                Bearer header). To call secured endpoints from this page: call \
                                POST /api/auth/signin, copy the value of that cookie from the response, \
                                then click "Authorize" below and paste it in."""
                                .formatted(jwtCookieName))
                        .contact(new Contact().name("E-Comm API")))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(jwtCookieName)));
    }
}
