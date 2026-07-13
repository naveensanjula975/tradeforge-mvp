package com.tradeforge.order.domain;

/**
 * Order lifecycle states.
 *
 * <p>Valid transitions (enforced by {@link Order}):
 * <pre>
 * PENDING_VALIDATION → ACCEPTED
 * PENDING_VALIDATION → REJECTED       (terminal)
 * ACCEPTED           → PARTIALLY_FILLED
 * ACCEPTED           → FILLED         (terminal)
 * ACCEPTED           → CANCELLED      (terminal)
 * PARTIALLY_FILLED   → PARTIALLY_FILLED
 * PARTIALLY_FILLED   → FILLED         (terminal)
 * PARTIALLY_FILLED   → CANCELLED      (terminal)
 * </pre>
 */
public enum OrderStatus {

    PENDING_VALIDATION,
    ACCEPTED,
    PARTIALLY_FILLED,
    FILLED,
    REJECTED,
    CANCELLED;

    /**
     * Terminal states cannot be transitioned out of.
     */
    public boolean isTerminal() {
        return this == FILLED || this == REJECTED || this == CANCELLED;
    }
}
