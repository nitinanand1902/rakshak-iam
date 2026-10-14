package com.healthcare.service;

import com.healthcare.audit.AuditEventType;
import com.healthcare.config.AppProperties;
import com.healthcare.config.JwtProperties;
import com.healthcare.dto.request.LoginRequest;
import com.healthcare.dto.request.RegisterRequest;
import com.healthcare.dto.response.AuthResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.AppException;
import com.healthcare.exception.BusinessException;
import com.healthcare.repository.*;
import com.healthcare.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication service: registration, login, logout, token refresh.
 * <p>
 * Security controls implemented here:
 * - BCrypt password hashing
 * - Brute-force protection (account lock after N failed attempts)
 * - JWT access + refresh token issuance
 * - Token revocation on logout
 * - Active session tracking
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TechnicianRepository technicianRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       TechnicianRepository technicianRepository,
                       RevokedTokenRepository revokedTokenRepository,
                       ActiveSessionRepository activeSessionRepository,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       AppProperties appProperties,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.technicianRepository = technicianRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.activeSessionRepository = activeSessionRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.appProperties = appProperties;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Registers a new user and creates the corresponding role profile.
     *
     * @param request registration data
     * @param ipAddress caller's IP for audit logging
     * @return JWT tokens on success
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email '" + request.email() + "' is already registered");
        }

        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role: '" + request.role() +
                    "'. Valid values: PATIENT, DOCTOR, TECHNICIAN");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();
        user = userRepository.save(user);

        // Create role-specific profile
        createRoleProfile(user, request);

        // Issue tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), role.getAuthority());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        trackSession(user, accessToken, ipAddress);

        auditService.log(AuditEventType.USER_REGISTERED, user.getUsername(),
                "User", String.valueOf(user.getId()),
                "New user registered with role: " + role, ipAddress, true);

        log.info("User registered: {} with role: {}", user.getUsername(), role);
        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(), user.getUsername(), role,
                "Registration successful");
    }

    /**
     * Authenticates a user, applying brute-force protection.
     *
     * @param request login credentials
     * @param ipAddress caller's IP for audit logging
     * @return JWT tokens on success
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    auditService.log(AuditEventType.LOGIN_FAILED, request.username(),
                            "User", null, "User not found", ipAddress, false);
                    return new AppException("Invalid credentials", 401, "INVALID_CREDENTIALS");
                });

        // Check lock status
        if (user.isAccountLocked()) {
            if (user.getAccountLockedUntil() != null && LocalDateTime.now().isAfter(user.getAccountLockedUntil())) {
                // Auto-unlock after lock duration
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            } else {
                auditService.log(AuditEventType.LOGIN_FAILED, request.username(),
                        "User", String.valueOf(user.getId()), "Account locked", ipAddress, false);
                throw new AppException("Account is temporarily locked due to multiple failed login attempts. "
                        + "Try again after " + appProperties.security().lockDurationMinutes() + " minutes.",
                        423, "ACCOUNT_LOCKED");
            }
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            // Reset failed attempts on success
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            userRepository.save(user);

            String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().getAuthority());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
            trackSession(user, accessToken, ipAddress);

            auditService.log(AuditEventType.LOGIN_SUCCESS, user.getUsername(),
                    "User", String.valueOf(user.getId()), "Login successful", ipAddress, true);

            log.info("Login successful for user: {}", user.getUsername());
            return AuthResponse.of(accessToken, refreshToken,
                    jwtProperties.accessTokenExpirationMs(), user.getUsername(), user.getRole(),
                    "Login successful");

        } catch (BadCredentialsException ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            int maxAttempts = appProperties.security().maxFailedAttempts();
            if (attempts >= maxAttempts) {
                user.setAccountLocked(true);
                user.setAccountLockedUntil(LocalDateTime.now()
                        .plusMinutes(appProperties.security().lockDurationMinutes()));
                auditService.log(AuditEventType.ACCOUNT_LOCKED, user.getUsername(),
                        "User", String.valueOf(user.getId()),
                        "Account locked after " + attempts + " failed attempts", ipAddress, false);
                log.warn("Account locked for user: {} after {} failed attempts", user.getUsername(), attempts);
            }
            userRepository.save(user);

            auditService.log(AuditEventType.LOGIN_FAILED, user.getUsername(),
                    "User", String.valueOf(user.getId()),
                    "Failed attempt " + attempts + "/" + maxAttempts, ipAddress, false);

            throw new AppException("Invalid credentials",
                    401, "INVALID_CREDENTIALS");
        }
    }

    /**
     * Revokes the current access token (logout).
     *
     * @param token the raw JWT token to revoke
     * @param username the authenticated username for audit
     * @param ipAddress caller's IP
     */
    @Transactional
    public void logout(String token, String username, String ipAddress) {
        if (token != null && !token.isBlank()) {
            LocalDateTime expiry = jwtTokenProvider.getExpiration(token);
            RevokedToken revoked = RevokedToken.builder()
                    .token(token)
                    .revokedAt(LocalDateTime.now())
                    .expiresAt(expiry)
                    .build();
            revokedTokenRepository.save(revoked);
            // Remove active session
            activeSessionRepository.deleteByToken(token);
        }
        auditService.log(AuditEventType.LOGOUT, username,
                "Session", null, "User logged out", ipAddress, true);
        log.info("User logged out: {}", username);
    }

    /**
     * Issues new access token from a valid refresh token.
     *
     * @param refreshToken the refresh token
     * @param ipAddress caller's IP
     * @return new access token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken, String ipAddress) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException("Invalid or expired refresh token", 401, "INVALID_REFRESH_TOKEN");
        }
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", 404, "USER_NOT_FOUND"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(username, user.getRole().getAuthority());
        trackSession(user, newAccessToken, ipAddress);

        auditService.log(AuditEventType.TOKEN_REFRESHED, username,
                "Token", null, "Access token refreshed", ipAddress, true);

        return AuthResponse.of(newAccessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(), username, user.getRole(), "Token refreshed");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void createRoleProfile(User user, RegisterRequest request) {
        switch (user.getRole()) {
            case PATIENT -> {
                Patient patient = Patient.builder()
                        .user(user)
                        .fullName(request.fullName())
                        .countryCode(request.countryCode())
                        .build();
                patientRepository.save(patient);
            }
            case DOCTOR -> {
                Doctor doctor = Doctor.builder()
                        .user(user)
                        .fullName(request.fullName())
                        .build();
                doctorRepository.save(doctor);
            }
            case TECHNICIAN -> {
                Technician technician = Technician.builder()
                        .user(user)
                        .fullName(request.fullName())
                        .build();
                technicianRepository.save(technician);
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
}
