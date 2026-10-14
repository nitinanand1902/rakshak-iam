package com.healthcare.dto.response;

import com.healthcare.entity.Role;

/** Authentication response containing JWT tokens and user summary. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        Role role,
        String message
) {
    /** Convenience factory with token type pre-filled. */
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, String username, Role role, String message) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, username, role, message);
    }
}
