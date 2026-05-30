/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChronoClass API")
                        .description("Global Class Offering Booking System — " +
                                "A backend service for a global live-learning platform. " +
                                "Teachers create course offerings with sessions, " +
                                "and parents/students book them with full timezone support.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ChronoClass Team")
                                .email("developer@chronoclass.com"))
                        .license(new License()
                                .name("Evaluation Use Only")
                                .url("https://github.com/chronoclass")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}
