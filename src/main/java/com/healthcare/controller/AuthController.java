package com.healthcare.controller;

import com.healthcare.dto.request.LoginRequest;
import com.healthcare.dto.request.RegisterRequest;
import com.healthcare.dto.response.AuthResponse;
import com.healthcare.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller — public endpoints for register, login, logout, token refresh.
 * Business logic lives entirely in {@link AuthService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, logout, token refresh")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user account and role profile. Returns JWT tokens.")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, getClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates credentials. Returns JWT access + refresh tokens.")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the current access token.")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractToken(httpRequest);
        String username = authentication != null ? authentication.getName() : "unknown";
        authService.logout(token, username, getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Issues a new access token from a valid refresh token.")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(refreshToken, getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
