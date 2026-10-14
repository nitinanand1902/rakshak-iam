package com.healthcare.security;

import com.healthcare.exception.TokenException;
import com.healthcare.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter — runs once per request.
 * <p>
 * 1. Extracts Bearer token from Authorization header
 * 2. Validates signature and expiry
 * 3. Rejects REFRESH tokens used as ACCESS tokens
 * 4. Checks revocation list (for logged-out tokens)
 * 5. Sets Spring SecurityContext for downstream filters
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   RevokedTokenRepository revokedTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtTokenProvider.validateAndGetClaims(token);

                // Reject refresh tokens used as access tokens
                if (!jwtTokenProvider.isAccessToken(claims)) {
                    log.warn("Refresh token used as access token — rejected for: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // Check revocation blacklist (token value, not JTI, for simplicity)
                if (revokedTokenRepository.existsByToken(token)) {
                    log.warn("Revoked token presented for: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user='{}' role='{}' path='{}'", username, role, request.getRequestURI());
            }
        } catch (TokenException e) {
            log.debug("Token validation failed ({}): {}", request.getRequestURI(), e.getMessage());
            // Let Spring Security handle as unauthenticated — do NOT set auth context
        } catch (Exception e) {
            log.error("Unexpected error in JWT filter at {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
