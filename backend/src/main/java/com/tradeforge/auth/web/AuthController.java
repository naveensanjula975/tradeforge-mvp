package com.tradeforge.auth.web;

import com.tradeforge.auth.service.UserService;
import com.tradeforge.auth.web.dto.AuthResponse;
import com.tradeforge.auth.web.dto.LoginRequest;
import com.tradeforge.auth.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * <pre>
 * POST /api/v1/auth/register  — create a new trader account
 * POST /api/v1/auth/login     — authenticate and receive a JWT
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
