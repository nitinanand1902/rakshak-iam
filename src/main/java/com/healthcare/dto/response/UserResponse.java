package com.healthcare.dto.response;

import com.healthcare.entity.Role;
import java.time.LocalDateTime;

/** User profile summary response (safe to expose — no sensitive fields). */
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        boolean accountLocked,
        LocalDateTime createdAt
) {}
