package com.healthcare.service;

import com.healthcare.audit.AuditEventType;
import com.healthcare.entity.AuditLog;
import com.healthcare.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Centralized audit logging service.
 * <p>
 * Uses a new transaction (REQUIRES_NEW) so audit records persist even when
 * the calling transaction rolls back. Async support allows non-blocking audit writes.
 */
@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Synchronously logs an audit event in a new independent transaction.
     * Use this when you need guaranteed persistence within the same thread.
     *
     * @param eventType    type of event
     * @param username     acting user
     * @param resourceType resource class name (e.g. "Patient")
     * @param resourceId   resource identifier (may be null)
     * @param description  human-readable description
     * @param ipAddress    caller IP address
     * @param success      whether the action succeeded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditEventType eventType, String username, String resourceType,
                    String resourceId, String description, String ipAddress, boolean success) {
        try {
            AuditLog entry = AuditLog.builder()
                    .eventType(eventType.name())
                    .username(username)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .success(success)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit logging must NEVER propagate failures to the calling business operation
            log.error("Failed to persist audit log event={} user={}: {}", eventType, username, e.getMessage());
        }
    }

    /**
     * Asynchronously logs an audit event for non-critical paths.
     * Runs in a separate thread pool — fire and forget.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(AuditEventType eventType, String username, String resourceType,
                         String resourceId, String description, String ipAddress, boolean success) {
        log(eventType, username, resourceType, resourceId, description, ipAddress, success);
    }
}
