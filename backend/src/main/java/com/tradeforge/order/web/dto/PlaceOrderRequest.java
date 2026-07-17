package com.tradeforge.order.web.dto;

import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request to place a new limit order.
 */
public record PlaceOrderRequest(
        @NotBlank(message = "Client order ID is required")
        @Size(min = 1, max = 64, message = "Client order ID must be 1-64 characters")
        String clientOrderId,

        @NotBlank(message = "Symbol is required")
        @Size(min = 1, max = 20, message = "Symbol must be 1-20 characters")
        String symbol,

        @NotNull(message = "Side (BUY/SELL) is required")
        OrderSide side,

        @NotNull(message = "Order type is required")
        OrderType type,

        @NotNull(message = "Limit price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal limitPrice,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be positive")
        BigDecimal quantity
) {
}
