package com.tradeforge.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Old password is required")
        String oldPassword,

        @NotBlank(message = "New password must be at least 8 characters")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {}
