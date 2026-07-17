package com.tradeforge.account.web.dto;

import com.tradeforge.account.domain.Account;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID userId,
        BigDecimal cashBalance,
        BigDecimal reservedCash,
        BigDecimal availableCash
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getCashBalance(),
                account.getReservedCash(),
                account.availableCash()
        );
    }
}
