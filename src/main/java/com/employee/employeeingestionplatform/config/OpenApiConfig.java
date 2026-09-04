package com.employee.employeeingestionplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME =
            "bearerAuth";

    @Bean
    OpenAPI employeeIngestionOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title(
                                        "Employee Ingestion Platform API"
                                )
                                .description(
                                        """
                                        REST APIs for employee ingestion \
                                        through Excel and Kafka, batch \
                                        processing, upload tracking and \
                                        employee retrieval.
                                        """
                                )
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("Anshu Kumari")
                                )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(
                                                        SECURITY_SCHEME_NAME
                                                )
                                                .type(
                                                        SecurityScheme.Type.HTTP
                                                )
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }
}