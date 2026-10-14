package com.healthcare.dto.response;

import java.time.LocalDateTime;

/** Patient profile response. */
public record PatientResponse(
        Long id,
        Long userId,
        String username,
        String fullName,
        String bloodGroup,
        String countryCode,
        LocalDateTime createdAt
) {}
