package com.healthcare.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Login request DTO. */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
