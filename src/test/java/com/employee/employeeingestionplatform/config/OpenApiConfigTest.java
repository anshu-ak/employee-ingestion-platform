package com.employee.employeeingestionplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig =
            new OpenApiConfig();

    @Test
    void shouldCreateEmployeeIngestionOpenApiDefinition() {
        OpenAPI openAPI =
                openApiConfig.employeeIngestionOpenApi();

        SecurityScheme securityScheme = openAPI
                .getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfig.SECURITY_SCHEME_NAME);

        assertAll(
                () -> assertNotNull(openAPI.getInfo()),
                () -> assertEquals(
                        "Employee Ingestion Platform API",
                        openAPI.getInfo().getTitle()
                ),
                () -> assertEquals(
                        "1.0.0",
                        openAPI.getInfo().getVersion()
                ),
                () -> assertEquals(
                        "Anshu Kumari",
                        openAPI.getInfo()
                                .getContact()
                                .getName()
                ),
                () -> assertNotNull(openAPI.getComponents()),
                () -> assertNotNull(securityScheme),
                () -> assertEquals(
                        SecurityScheme.Type.HTTP,
                        securityScheme.getType()
                ),
                () -> assertEquals(
                        "bearer",
                        securityScheme.getScheme()
                ),
                () -> assertEquals(
                        "JWT",
                        securityScheme.getBearerFormat()
                )
        );
    }
}