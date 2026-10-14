package com.healthcare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties bound from application.properties.
 *
 * @param secret                    hex-encoded 64-char (256-bit) HMAC-SHA256 signing key
 * @param accessTokenExpirationMs   access token TTL in milliseconds (default: 15 min)
 * @param refreshTokenExpirationMs  refresh token TTL in milliseconds (default: 7 days)
 * @param issuer                    JWT issuer claim
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs,
        String issuer
) {}
