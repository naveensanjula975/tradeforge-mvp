package com.tradeforge.auth.config;

import com.tradeforge.common.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — stateless JWT, CORS disabled (configure per deployment),
 * CSRF disabled (stateless API), method-level security enabled.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final GlobalExceptionHandler globalExceptionHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, GlobalExceptionHandler globalExceptionHandler) {
        this.jwtAuthFilter          = jwtAuthFilter;
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Stateless REST API ────────────────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorization rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/ws/**"
                ).permitAll()

                // Admin-only
                .requestMatchers(HttpMethod.POST,  "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/v1/admin/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // ── Exception handling ────────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    globalExceptionHandler
                        .handleAuthentication(authException, request)
                        .getBody())
            )

            // ── JWT filter ────────────────────────────────────────────────────
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
