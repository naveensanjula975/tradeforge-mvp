package com.tradeforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TradeForge MVP — entry point.
 *
 * <p>A correctness-first mini electronic exchange featuring:
 * <ul>
 *   <li>Price-time priority matching engine</li>
 *   <li>Limit orders with partial fill support</li>
 *   <li>JWT-secured REST API</li>
 *   <li>Real-time WebSocket order book updates</li>
 *   <li>PostgreSQL persistence with Flyway migrations</li>
 * </ul>
 */
@SpringBootApplication
public class TradeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeForgeApplication.class, args);
    }
}
