package com.tradeforge.order.domain.vo;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;

import java.util.Objects;

/**
 * Immutable client-provided order identifier.
 * Must not be blank; max 64 characters; unique per user.
 */
public final class ClientOrderId {

    public static final int MAX_LENGTH = 64;

    private final String value;

    private ClientOrderId(String value) {
        this.value = value;
    }

    public static ClientOrderId of(String value) {
        Objects.requireNonNull(value, "ClientOrderId must not be null");
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.ORDER_CLIENT_ID_DUPLICATE,
                    "Client order ID must not be blank.");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new BusinessRuleException(ErrorCode.INVALID_REQUEST,
                    "Client order ID must not exceed " + MAX_LENGTH + " characters.");
        }
        return new ClientOrderId(trimmed);
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientOrderId c)) return false;
        return value.equals(c.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public String toString() { return value; }
}
