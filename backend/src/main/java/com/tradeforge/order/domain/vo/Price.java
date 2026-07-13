package com.tradeforge.order.domain.vo;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing a monetary price.
 * Price must be strictly positive.
 */
public final class Price implements Comparable<Price> {

    private final BigDecimal value;

    private Price(BigDecimal value) {
        this.value = value;
    }

    public static Price of(BigDecimal value) {
        Objects.requireNonNull(value, "Price value must not be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_PRICE,
                    "Price must be positive, but was: " + value);
        }
        return new Price(value.stripTrailingZeros());
    }

    public static Price of(String value) {
        return of(new BigDecimal(value));
    }

    public BigDecimal getValue() { return value; }

    /**
     * Check that this price is aligned to the given tick size.
     *
     * @throws BusinessRuleException if not aligned
     */
    public void requireOnTick(BigDecimal tickSize) {
        if (tickSize.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal[] divRem = value.divideAndRemainder(tickSize);
        if (divRem[1].compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_PRICE_NOT_ON_TICK,
                    "Price " + value + " is not a multiple of tick size " + tickSize);
        }
    }

    public boolean isGreaterThanOrEqual(Price other) {
        return this.value.compareTo(other.value) >= 0;
    }

    @Override
    public int compareTo(Price other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Price p)) return false;
        return value.compareTo(p.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
