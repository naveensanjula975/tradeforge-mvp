package com.tradeforge.account.domain;

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
 * A trader's holding in a single instrument.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code quantity >= 0}</li>
 *   <li>{@code reservedQuantity >= 0}</li>
 *   <li>{@code reservedQuantity <= quantity}</li>
 * </ul>
 */
@Entity
@Table(
    name = "positions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_positions_acct_instr",
        columnNames = {"account_id", "instrument_id"}
    )
)
@EntityListeners(AuditingEntityListener.class)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averagePrice = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Position() { /* JPA */ }

    public static Position create(UUID accountId, UUID instrumentId) {
        Position p = new Position();
        p.accountId         = accountId;
        p.instrumentId      = instrumentId;
        p.quantity          = BigDecimal.ZERO;
        p.reservedQuantity  = BigDecimal.ZERO;
        p.averagePrice      = BigDecimal.ZERO;
        return p;
    }

    public static Position create(UUID accountId, UUID instrumentId, BigDecimal quantity, BigDecimal avgPrice) {
        Position p = create(accountId, instrumentId);
        p.quantity     = quantity;
        p.averagePrice = avgPrice;
        return p;
    }

    // ── Domain methods ────────────────────────────────────────────────────────

    public BigDecimal availableQuantity() {
        return quantity.subtract(reservedQuantity);
    }

    public void reserveQuantity(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (availableQuantity().compareTo(amount) < 0) {
            throw new BusinessRuleException(ErrorCode.INSUFFICIENT_POSITION,
                    "Insufficient available position. Available: " + availableQuantity() + ", Required: " + amount);
        }
        reservedQuantity = reservedQuantity.add(amount);
    }

    public void releaseQuantity(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        reservedQuantity = reservedQuantity.subtract(amount).max(BigDecimal.ZERO);
    }

    public void applySellExecution(BigDecimal executionQty) {
        quantity         = quantity.subtract(executionQty);
        reservedQuantity = reservedQuantity.subtract(executionQty).max(BigDecimal.ZERO);
    }

    public void applyBuyExecution(BigDecimal executionQty, BigDecimal executionPrice) {
        // Weighted average price update
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            averagePrice = executionPrice;
        } else {
            BigDecimal totalCost   = averagePrice.multiply(quantity).add(executionPrice.multiply(executionQty));
            BigDecimal totalQty    = quantity.add(executionQty);
            averagePrice           = totalCost.divide(totalQty, 4, java.math.RoundingMode.HALF_UP);
        }
        quantity = quantity.add(executionQty);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public UUID getId()                    { return id; }
    public UUID getAccountId()             { return accountId; }
    public UUID getInstrumentId()          { return instrumentId; }
    public BigDecimal getQuantity()        { return quantity; }
    public BigDecimal getReservedQuantity(){ return reservedQuantity; }
    public BigDecimal getAveragePrice()    { return averagePrice; }
    public long getVersion()               { return version; }
    public Instant getCreatedAt()          { return createdAt; }
    public Instant getUpdatedAt()          { return updatedAt; }
}
