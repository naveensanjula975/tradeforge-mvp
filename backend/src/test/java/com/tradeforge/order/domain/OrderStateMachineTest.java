package com.tradeforge.order.domain;

import com.tradeforge.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the Order aggregate state machine and quantity invariants.
 * No Spring context — pure unit tests.
 */
@DisplayName("Order aggregate — state machine")
class OrderStateMachineTest {

    private static final UUID USER_ID       = UUID.randomUUID();
    private static final UUID INSTRUMENT_ID = UUID.randomUUID();

    private Order newOrder(BigDecimal qty) {
        return Order.create("CID-001", USER_ID, INSTRUMENT_ID,
                OrderSide.BUY, new BigDecimal("100.00"), qty);
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("new order starts as PENDING_VALIDATION")
    void newOrder_isPendingValidation() {
        Order order = newOrder(new BigDecimal("100"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_VALIDATION);
    }

    @Test
    @DisplayName("new order: filled=0, remaining=original")
    void newOrder_quantityInvariant() {
        BigDecimal qty   = new BigDecimal("100");
        Order      order = newOrder(qty);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo(qty);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── accept ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING_VALIDATION → ACCEPTED (accept)")
    void accept_fromPending_becomesAccepted() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("accept from non-PENDING_VALIDATION throws")
    void accept_fromAccepted_throws() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        assertThatThrownBy(order::accept).isInstanceOf(BusinessRuleException.class);
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING_VALIDATION → REJECTED (reject)")
    void reject_fromPending_becomesRejected() {
        Order order = newOrder(new BigDecimal("100"));
        order.reject("validation failed");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getStatus().isTerminal()).isTrue();
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ACCEPTED → CANCELLED (cancel)")
    void cancel_fromAccepted_becomesCancelled() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("PARTIALLY_FILLED → CANCELLED (cancel)")
    void cancel_fromPartiallyFilled_becomesCancelled() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.applyFill(new BigDecimal("40"));
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel from PENDING_VALIDATION throws")
    void cancel_fromPending_throws() {
        Order order = newOrder(new BigDecimal("100"));
        assertThatThrownBy(order::cancel).isInstanceOf(BusinessRuleException.class);
    }

    // ── applyFill ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("full fill: ACCEPTED → FILLED, remaining=0")
    void applyFill_fullFill_becomesFilledWithZeroRemaining() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.applyFill(new BigDecimal("100"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("partial fill: ACCEPTED → PARTIALLY_FILLED")
    void applyFill_partialFill_becomesPartiallyFilled() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.applyFill(new BigDecimal("40"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo("60");
        assertThat(order.getFilledQuantity()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("second partial fill accumulates correctly")
    void applyFill_twiceFilled_quantitiesAccumulate() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.applyFill(new BigDecimal("40"));
        order.applyFill(new BigDecimal("60"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo("100");
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("fill exceeding remaining quantity throws")
    void applyFill_exceedsRemaining_throws() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        assertThatThrownBy(() -> order.applyFill(new BigDecimal("101")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("remaining quantity never becomes negative")
    void applyFill_exactQty_remainingExactlyZero() {
        Order order = newOrder(new BigDecimal("50"));
        order.accept();
        order.applyFill(new BigDecimal("50"));
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("fill on terminal FILLED order throws")
    void applyFill_onFilledOrder_throws() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.applyFill(new BigDecimal("100"));
        assertThatThrownBy(() -> order.applyFill(new BigDecimal("10")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("fill on CANCELLED order throws")
    void applyFill_onCancelledOrder_throws() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();
        order.cancel();
        assertThatThrownBy(() -> order.applyFill(new BigDecimal("10")))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── Quantity invariant ────────────────────────────────────────────────────

    @Test
    @DisplayName("filled + remaining == original at all times")
    void quantityInvariant_maintainedAfterFills() {
        Order order = newOrder(new BigDecimal("100"));
        order.accept();

        order.applyFill(new BigDecimal("30"));
        BigDecimal sum1 = order.getFilledQuantity().add(order.getRemainingQuantity());
        assertThat(sum1).isEqualByComparingTo(order.getOriginalQuantity());

        order.applyFill(new BigDecimal("50"));
        BigDecimal sum2 = order.getFilledQuantity().add(order.getRemainingQuantity());
        assertThat(sum2).isEqualByComparingTo(order.getOriginalQuantity());
    }
}
