package com.tradeforge.auth.web.dto;

import java.time.Instant;
import java.util.UUID;
import com.tradeforge.auth.domain.User;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        String role,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
