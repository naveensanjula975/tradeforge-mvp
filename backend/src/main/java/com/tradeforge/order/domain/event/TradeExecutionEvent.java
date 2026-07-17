package com.tradeforge.order.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeExecutionEvent(
        UUID buyOrderId,
        UUID sellOrderId,
        UUID buyUserId,
        UUID sellUserId,
        String symbol,
        BigDecimal price,
        BigDecimal quantity,
        Instant timestamp
) {}
