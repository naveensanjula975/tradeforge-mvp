package com.tradeforge.instrument.web.dto;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalOrders,
        BigDecimal totalVolume,
        long activeUsers
) {}
