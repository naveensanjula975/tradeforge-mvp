package com.tradeforge.instrument.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentStatus;

/**
 * API response for a single instrument.
 */
public record InstrumentResponse(
        UUID id,
        String symbol,
        String name,
        InstrumentStatus status,
        BigDecimal tickSize,
        BigDecimal lotSize,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstrumentResponse from(Instrument i) {
        return new InstrumentResponse(
                i.getId(), i.getSymbol(), i.getName(), i.getStatus(),
                i.getTickSize(), i.getLotSize(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
