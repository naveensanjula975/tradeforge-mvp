package com.tradeforge.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.util.Optional;

/**
 * Enables JPA auditing for {@code @CreatedDate} / {@code @LastModifiedDate} fields.
 * Uses {@link Instant} (UTC) for all timestamps.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "instantDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider instantDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
