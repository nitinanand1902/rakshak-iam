package com.healthcare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 * Swagger UI available at: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rakshakIamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rakshak IAM System API")
                        .description("""
                                Production-grade Identity and Access Management demonstration.
                                
                                **Key Security Features:**
                                - JWT Authentication (access + refresh tokens)
                                - Role-Based Access Control (PATIENT, DOCTOR, TECHNICIAN)
                                - AES-256/GCM encryption for identity documents
                                - Country-agnostic identity document management (Aadhaar, SSN, PAN, Passport…)
                                - Brute-force protection with account locking
                                - Stateless token revocation (logout blacklist)
                                - Comprehensive audit logging
                                - IAM security posture evaluation
                                
                                **Default credentials (from DataInitializer):**
                                - Patient: `patient1` / `Patient@1234`
                                - Doctor: `doctor1` / `Doctor@1234`
                                - Technician: `tech1` / `Tech@1234`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rakshak IAM Team")
                                .email("iam@rakshak.com"))
                        .license(new License()
                                .name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter the JWT token from the /api/v1/auth/login response")));
    }
}
