package com.healthcare.controller;

import com.healthcare.dto.response.AuditLogResponse;
import com.healthcare.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Audit log controller — read-only, doctor-only access.
 * Allows reviewing the security and activity trail.
 */
@RestController
@RequestMapping("/api/v1/audit")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logs", description = "Security audit trail — doctor-only read access")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List all audit logs — doctors only, paginated")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(auditQueryService.getAllLogs(pageable));
    }

    @GetMapping("/users/{username}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get audit logs by username — doctors only")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByUsername(
            @PathVariable String username,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditQueryService.getLogsByUsername(username, pageable));
    }

    @GetMapping("/events/{eventType}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get audit logs by event type — doctors only")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByEventType(
            @PathVariable String eventType,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditQueryService.getLogsByEventType(eventType, pageable));
    }
}
