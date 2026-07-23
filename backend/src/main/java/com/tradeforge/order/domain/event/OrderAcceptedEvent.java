package com.tradeforge.order.domain.event;

import java.util.UUID;

public record OrderAcceptedEvent(UUID orderId, String correlationId) {
    public OrderAcceptedEvent(UUID orderId) {
        this(orderId, null);
    }
}
