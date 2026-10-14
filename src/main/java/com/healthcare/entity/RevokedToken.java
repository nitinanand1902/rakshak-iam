package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Stores revoked JWT tokens to support stateless logout.
 * The JwtAuthenticationFilter checks this table on every request.
 * Expired records can be purged periodically.
 */
@Entity
@Table(
    name = "revoked_tokens",
    indexes = {
        @Index(name = "idx_revoked_token", columnList = "token", unique = true),
        @Index(name = "idx_revoked_expires", columnList = "expires_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024, unique = true)
    private String token;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
