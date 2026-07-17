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
 * A trader's cash wallet.
 *
 * <p>Invariants (enforced by domain methods and DB constraints):
 * <ul>
 *   <li>cashBalance &ge; 0</li>
 *   <li>reservedCash &ge; 0</li>
 *   <li>reservedCash &le; cashBalance</li>
 * </ul>
 */
@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashBalance;

    @Column(name = "reserved_cash", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedCash = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() { /* JPA */ }

    public static Account create(UUID userId, BigDecimal initialBalance) {
        Account a = new Account();
        a.userId       = userId;
        a.cashBalance  = initialBalance;
        a.reservedCash = BigDecimal.ZERO;
        return a;
    }

    // ── Domain methods ────────────────────────────────────────────────────────

    /** Available cash that can be used for new buy orders. */
    public BigDecimal availableCash() {
        return cashBalance.subtract(reservedCash);
    }

    /**
     * Reserve cash for a pending BUY order.
     *
     * @throws BusinessRuleException if insufficient available funds
     */
    public void reserveCash(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_PRICE, "Reserve amount must be positive.");
        }
        if (availableCash().compareTo(amount) < 0) {
            throw new BusinessRuleException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Insufficient available cash. Available: " + availableCash() + ", Required: " + amount);
        }
        reservedCash = reservedCash.add(amount);
    }

    /**
     * Release previously reserved cash (on cancellation or partial fill adjustment).
     */
    public void releaseCash(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        reservedCash = reservedCash.subtract(amount).max(BigDecimal.ZERO);
    }

    /**
     * Apply a BUY execution: deduct the executed cost from balance and reserved cash.
     */
    public void applyBuyExecution(BigDecimal executionPrice, BigDecimal executionQty, BigDecimal limitPrice) {
        BigDecimal cost = executionPrice.multiply(executionQty);
        BigDecimal reservedAmount = limitPrice.multiply(executionQty);
        cashBalance  = cashBalance.subtract(cost);
        reservedCash = reservedCash.subtract(reservedAmount).max(BigDecimal.ZERO);
    }

    /**
     * Apply a SELL execution: credit proceeds to balance.
     */
    public void applySellProceeds(BigDecimal executionPrice, BigDecimal executionQty) {
        BigDecimal proceeds = executionPrice.multiply(executionQty);
        cashBalance = cashBalance.add(proceeds);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public UUID getId()              { return id; }
    public UUID getUserId()          { return userId; }
    public BigDecimal getCashBalance()  { return cashBalance; }
    public BigDecimal getReservedCash() { return reservedCash; }
    public long getVersion()         { return version; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
}
