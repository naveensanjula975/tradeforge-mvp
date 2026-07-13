package com.tradeforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring ApplicationContext loads correctly.
 *
 * <p>Uses the {@code test} profile which connects to a Testcontainers PostgreSQL
 * instance automatically via the Testcontainers JDBC URL.
 */
@SpringBootTest
@ActiveProfiles("test")
class TradeForgeApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the ApplicationContext wired correctly.
    }
}
