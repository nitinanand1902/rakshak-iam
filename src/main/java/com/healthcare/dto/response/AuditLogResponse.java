package com.healthcare.dto.response;

import java.time.LocalDateTime;

/** Audit log entry response. */
public record AuditLogResponse(
        Long id,
        String eventType,
        String username,
        String resourceType,
        String resourceId,
        String description,
        String ipAddress,
        boolean success,
        LocalDateTime timestamp
) {}
