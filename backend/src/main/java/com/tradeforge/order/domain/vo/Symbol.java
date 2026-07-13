package com.tradeforge.order.domain.vo;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;

import java.util.Objects;

/**
 * Immutable instrument symbol value object. Normalised to uppercase.
 */
public final class Symbol {

    private final String value;

    private Symbol(String value) {
        this.value = value;
    }

    public static Symbol of(String value) {
        Objects.requireNonNull(value, "Symbol must not be null");
        String normalised = value.strip().toUpperCase();
        if (normalised.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_REQUEST, "Symbol must not be blank.");
        }
        return new Symbol(normalised);
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Symbol s)) return false;
        return value.equals(s.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public String toString() { return value; }
}
