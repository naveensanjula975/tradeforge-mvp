package com.tradeforge.auth.web.dto;

import java.util.UUID;

/**
 * Response body for successful login and registration.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UUID userId,
        String email,
        String role
) {
    public static AuthResponse of(String token, long expiresInMs, UUID userId, String email, String role) {
        return new AuthResponse(token, "Bearer", expiresInMs, userId, email, role);
    }
}
