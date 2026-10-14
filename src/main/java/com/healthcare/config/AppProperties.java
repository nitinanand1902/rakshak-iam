package com.healthcare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-wide security properties.
 *
 * @param security brute-force protection settings
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security) {

    /**
     * @param maxFailedAttempts  max consecutive failures before account lock
     * @param lockDurationMinutes  minutes the account stays locked
     */
    public record Security(int maxFailedAttempts, int lockDurationMinutes) {}
}
