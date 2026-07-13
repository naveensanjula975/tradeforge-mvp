package com.tradeforge.order.domain;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order aggregate root — the core domain object of the TradeForge system.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>{@code filledQty + remainingQty == originalQty} always.</li>
 *   <li>{@code remainingQty >= 0} always.</li>
 *   <li>Terminal states ({@link OrderStatus#isTerminal()}) cannot be transitioned out of.</li>
 * </ul>
 *
 * <h3>State transitions</h3>
 * <pre>
 * PENDING_VALIDATION → ACCEPTED          (accept)
 * PENDING_VALIDATION → REJECTED          (reject)
 * ACCEPTED           → PARTIALLY_FILLED  (applyFill)
 * ACCEPTED           → FILLED            (applyFill — fully)
 * ACCEPTED           → CANCELLED         (cancel)
 * PARTIALLY_FILLED   → PARTIALLY_FILLED  (applyFill — partial)
 * PARTIALLY_FILLED   → FILLED            (applyFill — remainder consumed)
 * PARTIALLY_FILLED   → CANCELLED         (cancel)
 * </pre>
 */
@Entity
@Table(
    name = "orders",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_orders_user_client_order_id",
        columnNames = {"user_id", "client_order_id"}
    )
)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "client_order_id", nullable = false, length = 64)
    private String clientOrderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    @Column(name = "limit_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal limitPrice;

    @Column(name = "original_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal originalQuantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal remainingQuantity;

    @Column(name = "filled_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private OrderStatus status;

    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() { /* JPA */ }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Order create(
            String clientOrderId,
            UUID userId,
            UUID instrumentId,
            OrderSide side,
            BigDecimal limitPrice,
            BigDecimal quantity) {
        Order o = new Order();
        o.clientOrderId      = clientOrderId;
        o.userId             = userId;
        o.instrumentId       = instrumentId;
        o.side               = side;
        o.type               = OrderType.LIMIT;
        o.limitPrice         = limitPrice;
        o.originalQuantity   = quantity;
        o.remainingQuantity  = quantity;
        o.filledQuantity     = BigDecimal.ZERO;
        o.status             = OrderStatus.PENDING_VALIDATION;
        return o;
    }

    // ── State machine ─────────────────────────────────────────────────────────

    /** Transition PENDING_VALIDATION → ACCEPTED. */
    public void accept() {
        requireTransition(OrderStatus.PENDING_VALIDATION, OrderStatus.ACCEPTED);
        this.status = OrderStatus.ACCEPTED;
    }

    /** Transition PENDING_VALIDATION → REJECTED. */
    public void reject(String reason) {
        requireTransition(OrderStatus.PENDING_VALIDATION, OrderStatus.REJECTED);
        this.status = OrderStatus.REJECTED;
    }

    /** Transition ACCEPTED or PARTIALLY_FILLED → CANCELLED. */
    public void cancel() {
        if (status != OrderStatus.ACCEPTED && status != OrderStatus.PARTIALLY_FILLED) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_TRANSITION,
                    "Cannot cancel order in status " + status + ". Order id: " + id);
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Apply an execution fill. Updates quantities and transitions state.
     *
     * @param executedQty the quantity filled in this execution (must be &le; remainingQuantity)
     * @throws BusinessRuleException if fill would create negative remaining quantity or order is terminal
     */
    public void applyFill(BigDecimal executedQty) {
        if (status.isTerminal()) {
            throw new BusinessRuleException(ErrorCode.ORDER_ALREADY_TERMINAL,
                    "Cannot fill a terminal order. Status: " + status + ", id: " + id);
        }
        if (status != OrderStatus.ACCEPTED && status != OrderStatus.PARTIALLY_FILLED) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_TRANSITION,
                    "Cannot fill order in status " + status);
        }
        if (executedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_QUANTITY,
                    "Executed quantity must be positive.");
        }
        if (executedQty.compareTo(remainingQuantity) > 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_QUANTITY,
                    "Executed quantity " + executedQty + " exceeds remaining quantity " + remainingQuantity);
        }

        filledQuantity    = filledQuantity.add(executedQty);
        remainingQuantity = remainingQuantity.subtract(executedQty);

        // Invariant: filledQty + remainingQty == originalQty
        assert filledQuantity.add(remainingQuantity).compareTo(originalQuantity) == 0;

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    /** Assign sequence number (called once by matching engine before entering order book). */
    public void assignSequenceNumber(long seq) {
        if (this.sequenceNumber != null) {
            throw new IllegalStateException("Sequence number already assigned: " + this.sequenceNumber);
        }
        this.sequenceNumber = seq;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireTransition(OrderStatus from, OrderStatus to) {
        if (status.isTerminal()) {
            throw new BusinessRuleException(ErrorCode.ORDER_ALREADY_TERMINAL,
                    "Order is already in terminal state " + status + ". Cannot transition to " + to);
        }
        if (status != from) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_TRANSITION,
                    "Invalid transition from " + status + " to " + to + ". Expected current status: " + from);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public UUID getId()                    { return id; }
    public String getClientOrderId()       { return clientOrderId; }
    public UUID getUserId()                { return userId; }
    public UUID getInstrumentId()          { return instrumentId; }
    public OrderSide getSide()             { return side; }
    public OrderType getType()             { return type; }
    public BigDecimal getLimitPrice()      { return limitPrice; }
    public BigDecimal getOriginalQuantity()  { return originalQuantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public BigDecimal getFilledQuantity()    { return filledQuantity; }
    public OrderStatus getStatus()         { return status; }
    public Long getSequenceNumber()        { return sequenceNumber; }
    public long getVersion()               { return version; }
    public Instant getCreatedAt()          { return createdAt; }
    public Instant getUpdatedAt()          { return updatedAt; }
}
