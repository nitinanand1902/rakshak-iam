package com.healthcare.security;

import com.healthcare.config.JwtProperties;
import com.healthcare.exception.TokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider using JJWT 0.12.x.
 * <p>
 * Issues signed HS256 access tokens (15 min) and refresh tokens (7 days).
 * Token type is embedded as a claim to prevent refresh token misuse.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Secret is stored as a hex string in properties; decode it
        byte[] keyBytes = hexStringToBytes(jwtProperties.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a short-lived access token.
     *
     * @param username  the authenticated username (becomes JWT subject)
     * @param authority the Spring Security authority string (e.g. ROLE_PATIENT)
     * @return compact JWT string
     */
    public String generateAccessToken(String username, String authority) {
        return buildToken(username, authority, jwtProperties.accessTokenExpirationMs(), "ACCESS");
    }

    /**
     * Generates a long-lived refresh token.
     *
     * @param username the authenticated username
     * @return compact JWT refresh token string
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, null, jwtProperties.refreshTokenExpirationMs(), "REFRESH");
    }

    /**
     * Generates a short-lived (5-minute) provisioning token for the SSO onboarding flow.
     * This token is only valid for {@code POST /api/v1/auth/sso/complete}.
     *
     * @param username the partial (not yet fully provisioned) SSO user's username
     * @return compact JWT provisioning token string
     */
    public String generateProvisioningToken(String username) {
        // 5 minutes in milliseconds
        return buildToken(username, null, 5 * 60 * 1000L, "PROVISIONING");
    }

    /**
     * Validates token signature, expiry, and structure. Throws {@link TokenException} on failure.
     *
     * @param token compact JWT string
     * @return parsed Claims
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            throw new TokenException("Token has expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new TokenException("Unsupported token format");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new TokenException("Malformed token");
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new TokenException("Invalid token signature");
        } catch (IllegalArgumentException e) {
            log.warn("Empty or null JWT token");
            throw new TokenException("Token is missing or empty");
        }
    }

    /**
     * Validates a token without throwing — used by refresh flow.
     *
     * @param token compact JWT string
     * @return true if valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (TokenException e) {
            return false;
        }
    }

    /** Extracts the username (subject) from a token. */
    public String getUsernameFromToken(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /** Returns true if this is an ACCESS token (not a refresh token). */
    public boolean isAccessToken(Claims claims) {
        return "ACCESS".equals(claims.get("tokenType", String.class));
    }

    /**
     * Returns the expiration as {@link LocalDateTime}.
     * Used to populate RevokedToken.expiresAt.
     */
    public LocalDateTime getExpiration(String token) {
        Date exp = validateAndGetClaims(token).getExpiration();
        return exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    // -------------------------------------------------------------------------
    private String buildToken(String username, String authority, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.issuer())
                .subject(username)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey);

        if (authority != null) {
            // Strip ROLE_ prefix for compact storage; reader adds it back
            String role = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private static byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
