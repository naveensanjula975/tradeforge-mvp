package com.tradeforge.order.domain.event;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId, String correlationId) {
    public OrderCancelledEvent(UUID orderId) {
        this(orderId, null);
    }
}
