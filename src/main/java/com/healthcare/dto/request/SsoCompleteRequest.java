package com.healthcare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for POST /api/v1/auth/sso/complete.
 * Sent by the frontend after the user selects their role on the onboarding page.
 *
 * @param provisioningToken the short-lived PROVISIONING JWT issued after SSO login
 * @param role              chosen role: PATIENT, DOCTOR, or TECHNICIAN
 */
public record SsoCompleteRequest(
        @NotBlank(message = "provisioningToken is required")
        String provisioningToken,

        @NotBlank(message = "role is required")
        @Pattern(regexp = "PATIENT|DOCTOR|TECHNICIAN", message = "role must be PATIENT, DOCTOR, or TECHNICIAN")
        String role
) {}
