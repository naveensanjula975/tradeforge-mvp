package com.tradeforge.order.domain.vo;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object representing an order quantity.
 * Quantity must be strictly positive.
 */
public final class Quantity implements Comparable<Quantity> {

    private final BigDecimal value;

    private Quantity(BigDecimal value) {
        this.value = value;
    }

    public static Quantity of(BigDecimal value) {
        Objects.requireNonNull(value, "Quantity value must not be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_QUANTITY,
                    "Quantity must be positive, but was: " + value);
        }
        return new Quantity(value.stripTrailingZeros());
    }

    public static Quantity of(String value) {
        return of(new BigDecimal(value));
    }

    public BigDecimal getValue() { return value; }

    /**
     * Check that this quantity is aligned to the given lot size.
     *
     * @throws BusinessRuleException if not aligned
     */
    public void requireOnLot(BigDecimal lotSize) {
        if (lotSize.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal[] divRem = value.divideAndRemainder(lotSize);
        if (divRem[1].compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_QUANTITY_NOT_ON_LOT,
                    "Quantity " + value + " is not a multiple of lot size " + lotSize);
        }
    }

    public Quantity subtract(Quantity other) {
        BigDecimal result = value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_QUANTITY,
                    "Quantity subtraction resulted in a negative value.");
        }
        return result.compareTo(BigDecimal.ZERO) == 0
                ? ZERO_SENTINEL
                : new Quantity(result.stripTrailingZeros());
    }

    public Quantity add(Quantity other) {
        return new Quantity(value.add(other.value).stripTrailingZeros());
    }

    public boolean isLessThan(Quantity other) {
        return this.value.compareTo(other.value) < 0;
    }

    public boolean isLessThanOrEqual(Quantity other) {
        return this.value.compareTo(other.value) <= 0;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    // Internal zero sentinel used by subtract — not for public use
    private static final Quantity ZERO_SENTINEL = new Quantity(BigDecimal.ZERO) {
        @Override public boolean isZero() { return true; }
    };

    @Override
    public int compareTo(Quantity other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity q)) return false;
        return value.compareTo(q.value) == 0;
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
