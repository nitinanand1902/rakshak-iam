package com.healthcare.repository;

import com.healthcare.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for querying audit logs. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    Page<AuditLog> findByEventType(String eventType, Pageable pageable);

    Page<AuditLog> findByUsernameAndEventType(String username, String eventType, Pageable pageable);
}
