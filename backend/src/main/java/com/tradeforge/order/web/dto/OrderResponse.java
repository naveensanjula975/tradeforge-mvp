package com.tradeforge.order.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.domain.OrderType;

/**
 * API response for a single order.
 */
public record OrderResponse(
        UUID id,
        String clientOrderId,
        UUID userId,
        UUID instrumentId,
        OrderSide side,
        OrderType type,
        BigDecimal limitPrice,
        BigDecimal originalQuantity,
        BigDecimal remainingQuantity,
        BigDecimal filledQuantity,
        OrderStatus status,
        Long sequenceNumber,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getClientOrderId(),
                order.getUserId(),
                order.getInstrumentId(),
                order.getSide(),
                order.getType(),
                order.getLimitPrice(),
                order.getOriginalQuantity(),
                order.getRemainingQuantity(),
                order.getFilledQuantity(),
                order.getStatus(),
                order.getSequenceNumber(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
