package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks active login sessions for audit and concurrent session management.
 */
@Entity
@Table(
    name = "active_sessions",
    indexes = {
        @Index(name = "idx_session_user", columnList = "user_id"),
        @Index(name = "idx_session_token", columnList = "token", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1024, unique = true)
    private String token;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
