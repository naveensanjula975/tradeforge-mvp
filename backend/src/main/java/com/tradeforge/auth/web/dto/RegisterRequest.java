package com.tradeforge.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/register}.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required.")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be a valid email address.")
        @Size(max = 254, message = "Email must not exceed 254 characters.")
        String email,

        /**
         * Strong password:
         * - At least 8 characters
         * - At least one uppercase letter
         * - At least one lowercase letter
         * - At least one digit
         * - At least one special character
         */
        @NotBlank(message = "Password is required.")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, digit, and special character."
        )
        String password
) {}
