package com.healthcare.service;

import com.healthcare.audit.AuditEventType;
import com.healthcare.config.JwtProperties;
import com.healthcare.dto.response.AuthResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.AppException;
import com.healthcare.exception.BusinessException;
import com.healthcare.repository.*;
import com.healthcare.security.JwtTokenProvider;
import com.healthcare.security.sso.SsoUserInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SSO Authentication Service.
 *
 * <p>Handles the two-phase SSO onboarding flow:
 * <ol>
 *   <li><b>Phase 1</b> {@link #handleSsoLogin} — called by the OIDC/SAML success handler.
 *       Looks up the user by {@code providerSub}. If found, issues a normal JWT pair.
 *       If new, creates a partial User (no role, {@code provisioned=false}) and issues
 *       a short-lived PROVISIONING token that only allows {@code /api/v1/auth/sso/complete}.</li>
 *   <li><b>Phase 2</b> {@link #completeProvisioning} — called by the user after picking their role
 *       on the onboarding page. Validates the provisioning token, assigns the chosen role,
 *       creates the role-profile entity, marks {@code provisioned=true}, and issues a full JWT pair.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoAuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TechnicianRepository technicianRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;
    private final JwtProperties jwtProperties;

    @Value("${app.sso.frontend-callback-url:http://localhost:5173/sso-callback}")
    private String frontendCallbackUrl;

    // -------------------------------------------------------------------------
    // Phase 1: Initial SSO login
    // -------------------------------------------------------------------------

    /**
     * Called after successful OIDC or SAML authentication.
     * Returns the frontend redirect URL containing either a provisioning token
     * (new user) or the full JWT pair (returning user).
     *
     * @param ssoInfo   normalized user info from the IdP
     * @param ipAddress client IP for audit logging
     * @return redirect URL for the frontend
     */
    @Transactional
    public String handleSsoLogin(SsoUserInfo ssoInfo, String ipAddress) {
        try {
            // --- Returning user ---
            return userRepository.findByProviderSub(ssoInfo.providerSub())
                    .map(user -> handleReturningUser(user, ssoInfo, ipAddress))
                    .orElseGet(() -> handleNewUser(ssoInfo, ipAddress));
        } catch (Exception ex) {
            log.error("SSO login error for sub={}: {}", ssoInfo.providerSub(), ex.getMessage());
            AuditEventType failType = "SAML".equals(ssoInfo.provider())
                    ? AuditEventType.SAML_LOGIN_FAILED
                    : AuditEventType.SSO_LOGIN_FAILED;
            auditService.log(failType, ssoInfo.email(),
                    "SSO", null, "SSO login failed: " + ex.getMessage(), ipAddress, false);
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Phase 2: Role selection / provisioning completion
    // -------------------------------------------------------------------------

    /**
     * Completes the SSO onboarding after the user selects their role.
     *
     * @param provisioningToken the short-lived PROVISIONING JWT issued in phase 1
     * @param roleName          the chosen role ("PATIENT", "DOCTOR", "TECHNICIAN")
     * @param ipAddress         client IP for audit logging
     * @return full JWT access + refresh token pair
     */
    @Transactional
    public AuthResponse completeProvisioning(String provisioningToken, String roleName, String ipAddress) {
        // Validate token
        if (!jwtTokenProvider.validateToken(provisioningToken)) {
            throw new AppException("Invalid or expired provisioning token", 401, "INVALID_PROVISIONING_TOKEN");
        }
        Claims claims = jwtTokenProvider.validateAndGetClaims(provisioningToken);
        if (!"PROVISIONING".equals(claims.get("tokenType", String.class))) {
            throw new AppException("Token is not a provisioning token", 401, "INVALID_TOKEN_TYPE");
        }

        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", 404, "USER_NOT_FOUND"));

        if (user.isProvisioned()) {
            throw new BusinessException("User has already completed onboarding. Please log in normally.");
        }

        // Assign role
        Role role;
        try {
            role = Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role: '" + roleName + "'. Valid values: PATIENT, DOCTOR, TECHNICIAN");
        }
        user.setRole(role);
        user.setProvisioned(true);
        userRepository.save(user);

        // Create role-specific profile
        createRoleProfile(user);

        // Issue full JWT pair
        String accessToken  = jwtTokenProvider.generateAccessToken(username, role.getAuthority());
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        trackSession(user, accessToken, ipAddress);

        auditService.log(AuditEventType.SSO_PROVISIONING_COMPLETED, username,
                "User", String.valueOf(user.getId()),
                "SSO onboarding completed. Role assigned: " + role, ipAddress, true);

        log.info("SSO provisioning completed for user: {} with role: {}", username, role);
        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(), username, role, "SSO onboarding complete");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String handleReturningUser(User user, SsoUserInfo ssoInfo, String ipAddress) {
        // Returning SSO user who hasn't finished onboarding yet
        if (!user.isProvisioned()) {
            String provisioningToken = jwtTokenProvider.generateProvisioningToken(user.getUsername());
            auditService.log(AuditEventType.SSO_PROVISIONING_STARTED, user.getUsername(),
                    "User", String.valueOf(user.getId()),
                    "Returning user resumed SSO onboarding", ipAddress, true);
            return frontendCallbackUrl + "?token=" + provisioningToken + "&state=provisioning";
        }

        // Fully provisioned returning user — issue full tokens
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().getAuthority());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        trackSession(user, accessToken, ipAddress);

        AuditEventType successType = "SAML".equals(ssoInfo.provider())
                ? AuditEventType.SAML_LOGIN_SUCCESS
                : AuditEventType.SSO_LOGIN_SUCCESS;
        auditService.log(successType, user.getUsername(),
                "User", String.valueOf(user.getId()),
                "SSO login via " + ssoInfo.provider(), ipAddress, true);

        log.info("SSO login successful for returning user: {} via {}", user.getUsername(), ssoInfo.provider());
        return frontendCallbackUrl + "?accessToken=" + accessToken + "&refreshToken=" + refreshToken
                + "&username=" + user.getUsername() + "&role=" + user.getRole().name() + "&state=authenticated";
    }

    private String handleNewUser(SsoUserInfo ssoInfo, String ipAddress) {
        // Derive a unique username from email or display name
        String baseUsername = deriveUsername(ssoInfo);
        String username     = ensureUniqueUsername(baseUsername);

        // Email may already be registered with a LOCAL account → link accounts
        User user = userRepository.findByEmail(ssoInfo.email())
                .map(existing -> {
                    // Link existing LOCAL account to the SSO provider
                    existing.setProviderSub(ssoInfo.providerSub());
                    existing.setAuthProvider(ssoInfo.provider());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    // Brand new user — create a partial account with no role yet
                    User newUser = User.builder()
                            .username(username)
                            .email(ssoInfo.email())
                            .password("") // SSO users have no local password
                            .authProvider(ssoInfo.provider())
                            .providerSub(ssoInfo.providerSub())
                            .provisioned(false) // awaiting role selection
                            .build();
                    return userRepository.save(newUser);
                });

        // Issue short-lived provisioning token
        String provisioningToken = jwtTokenProvider.generateProvisioningToken(user.getUsername());

        auditService.log(AuditEventType.SSO_PROVISIONING_STARTED, user.getUsername(),
                "User", String.valueOf(user.getId()),
                "New SSO user from " + ssoInfo.provider() + " — awaiting role selection", ipAddress, true);

        log.info("New SSO user started provisioning: {} via {}", user.getUsername(), ssoInfo.provider());
        return frontendCallbackUrl + "?token=" + provisioningToken + "&state=provisioning"
                + "&displayName=" + encodeQueryParam(ssoInfo.displayName());
    }

    private void createRoleProfile(User user) {
        switch (user.getRole()) {
            case PATIENT -> {
                Patient p = Patient.builder()
                        .user(user)
                        .fullName(extractDisplayName(user))
                        .countryCode("IN")
                        .build();
                patientRepository.save(p);
            }
            case DOCTOR -> {
                Doctor d = Doctor.builder()
                        .user(user)
                        .fullName(extractDisplayName(user))
                        .build();
                doctorRepository.save(d);
            }
            case TECHNICIAN -> {
                Technician t = Technician.builder()
                        .user(user)
                        .fullName(extractDisplayName(user))
                        .build();
                technicianRepository.save(t);
            }
        }
    }

    private void trackSession(User user, String token, String ipAddress) {
        ActiveSession session = ActiveSession.builder()
                .user(user)
                .token(token)
                .ipAddress(ipAddress)
                .loginTime(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.accessTokenExpirationMs() / 1000))
                .build();
        activeSessionRepository.save(session);
    }

    private String deriveUsername(SsoUserInfo info) {
        if (info.email() != null) {
            return info.email().split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        }
        return info.displayName() != null
                ? info.displayName().replaceAll("\\s+", "_").toLowerCase()
                : "sso_user";
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private String extractDisplayName(User user) {
        // Best-effort: derive from username until profile is updated
        return user.getUsername().replace("_", " ");
    }

    private String encodeQueryParam(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
