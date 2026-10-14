package com.healthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import com.healthcare.config.AppProperties;
import com.healthcare.config.JwtProperties;
import com.healthcare.config.SamlProperties;

/**
 * Rakshak IAM System - Entry Point
 * <p>
 * Demonstrates enterprise IAM patterns: JWT auth, RBAC, AES-256 encryption,
 * audit logging, brute-force protection, token revocation, and security evaluation.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class, SamlProperties.class})
public class HealthcareIamApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthcareIamApplication.class, args);
    }
}
