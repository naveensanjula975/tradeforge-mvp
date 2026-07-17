package com.tradeforge.account.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountStatementResponse(
        UUID accountId,
        BigDecimal balance,
        BigDecimal buyingPower,
        BigDecimal totalPositionValue,
        BigDecimal totalPortfolioValue,
        BigDecimal totalPnL
) {}
