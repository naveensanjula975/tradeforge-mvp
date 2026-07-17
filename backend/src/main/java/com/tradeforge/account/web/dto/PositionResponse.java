package com.tradeforge.account.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PositionResponse(
        UUID id,
        UUID accountId,
        UUID instrumentId,
        String symbol,
        BigDecimal quantity,
        BigDecimal reservedQuantity,
        BigDecimal averagePrice,
        BigDecimal marketValue
) {}
