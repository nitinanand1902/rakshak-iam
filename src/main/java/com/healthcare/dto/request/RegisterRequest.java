package com.healthcare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO.
 * Role is a String so new roles can be added without changing this record.
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        /**
         * Role string (e.g. "PATIENT", "DOCTOR", "TECHNICIAN").
         * Extensible: adding a new role needs no DTO change.
         */
        @NotBlank(message = "Role is required")
        String role,

        /** Optional full name */
        String fullName,

        /**
         * ISO 3166-1 alpha-2 country code (e.g. IN, US, CN).
         * Drives country-specific business rules and identity validation.
         */
        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 3)
        String countryCode
) {}
