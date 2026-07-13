package com.tradeforge.order.domain.vo;

/**
 * Immutable monotonically-increasing sequence number for price-time priority.
 * Assigned by {@link com.tradeforge.matching.service.SequenceNumberGenerator}
 * before an order enters the order book.
 */
public final class SequenceNumber implements Comparable<SequenceNumber> {

    private final long value;

    private SequenceNumber(long value) {
        this.value = value;
    }

    public static SequenceNumber of(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Sequence number must be non-negative, but was: " + value);
        }
        return new SequenceNumber(value);
    }

    public long getValue() { return value; }

    @Override
    public int compareTo(SequenceNumber other) {
        return Long.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SequenceNumber s)) return false;
        return value == s.value;
    }

    @Override
    public int hashCode() { return Long.hashCode(value); }

    @Override
    public String toString() { return String.valueOf(value); }
}
