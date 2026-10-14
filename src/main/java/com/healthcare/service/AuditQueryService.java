package com.healthcare.service;

import com.healthcare.dto.response.AuditLogResponse;
import com.healthcare.entity.AuditLog;
import com.healthcare.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side service for querying audit logs.
 * Write side is handled by {@link AuditService}.
 */
@Service
@Slf4j
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Returns all audit logs, paginated — doctor-only endpoint. */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toResponse);
    }

    /** Returns audit logs filtered by username. */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable).map(this::toResponse);
    }

    /** Returns audit logs filtered by event type. */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByEventType(String eventType, Pageable pageable) {
        return auditLogRepository.findByEventType(eventType, pageable).map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEventType(),
                maskUsername(log.getUsername()),
                log.getResourceType(),
                maskResourceId(log.getResourceId()),
                log.getDescription(),
                maskIpAddress(log.getIpAddress()),
                log.isSuccess(),
                log.getTimestamp()
        );
    }

    private String maskUsername(String username) {
        if (username == null || username.isBlank() || "unknown".equalsIgnoreCase(username)) {
            return "unknown";
        }
        if (username.length() <= 2) {
            return username.charAt(0) + "***";
        }
        return username.substring(0, 2) + "***";
    }

    private String maskResourceId(String resourceId) {
        if (resourceId == null || resourceId.isBlank() || "all".equalsIgnoreCase(resourceId)) {
            return resourceId;
        }
        return "redacted";
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        if (ipAddress.contains(":")) {
            return "****:****";
        }
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return "masked";
        }
        return parts[0] + "." + parts[1] + ".***.***";
    }
}
