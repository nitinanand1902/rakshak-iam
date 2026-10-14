package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Core user entity. Represents any system user regardless of role.
 * Role-specific profile data lives in Patient / Doctor / Technician entities.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Authentication provider for this account.
     * "LOCAL" for username/password accounts; "OIDC_GOOGLE", "OIDC_MICROSOFT",
     * "OIDC_GENERIC", or "SAML" for SSO-provisioned accounts.
     */
    @Column(length = 30)
    @Builder.Default
    private String authProvider = "LOCAL";

    /**
     * The IdP's unique subject identifier (e.g. Google UID, Microsoft OID, SAML NameID).
     * Used to look up existing SSO users on subsequent logins.
     */
    @Column(unique = true)
    private String providerSub;

    /**
     * Whether the SSO user has completed the role-selection onboarding step.
     * Always true for LOCAL accounts. Set to true once POST /sso/complete succeeds.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean provisioned = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    /** Time until which the account is locked due to brute-force detection. */
    private LocalDateTime accountLockedUntil;

    @Column(nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
