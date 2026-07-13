package com.tradeforge.instrument.domain;

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
 * Tradeable security (e.g. CAL, JKH, COMB).
 *
 * <p>Rules:
 * <ul>
 *   <li>Symbol is unique and immutable after creation.</li>
 *   <li>Tick size and lot size must be positive.</li>
 *   <li>Inactive instruments do not accept new orders.</li>
 * </ul>
 */
@Entity
@Table(
    name = "instruments",
    uniqueConstraints = @UniqueConstraint(name = "uq_instruments_symbol", columnNames = "symbol")
)
@EntityListeners(AuditingEntityListener.class)
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 20, updatable = false)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstrumentStatus status = InstrumentStatus.ACTIVE;

    @Column(name = "tick_size", nullable = false, precision = 19, scale = 8)
    private BigDecimal tickSize;

    @Column(name = "lot_size", nullable = false, precision = 19, scale = 8)
    private BigDecimal lotSize;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Instrument() { /* JPA */ }

    public static Instrument create(String symbol, String name, BigDecimal tickSize, BigDecimal lotSize) {
        if (tickSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_PRICE, "Tick size must be positive.");
        }
        if (lotSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(ErrorCode.ORDER_INVALID_QUANTITY, "Lot size must be positive.");
        }
        Instrument i = new Instrument();
        i.symbol   = symbol.toUpperCase().strip();
        i.name     = name.strip();
        i.tickSize = tickSize;
        i.lotSize  = lotSize;
        i.status   = InstrumentStatus.ACTIVE;
        return i;
    }

    // ── Domain methods ────────────────────────────────────────────────────────

    public void activate() {
        this.status = InstrumentStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = InstrumentStatus.INACTIVE;
    }

    public boolean isActive() {
        return InstrumentStatus.ACTIVE == status;
    }

    /**
     * Validates that the instrument can accept orders.
     *
     * @throws BusinessRuleException if inactive
     */
    public void requireActive() {
        if (!isActive()) {
            throw new BusinessRuleException(ErrorCode.INSTRUMENT_INACTIVE,
                    "Instrument '" + symbol + "' is not active and cannot accept orders.");
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public UUID getId()              { return id; }
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public InstrumentStatus getStatus() { return status; }
    public BigDecimal getTickSize()  { return tickSize; }
    public BigDecimal getLotSize()   { return lotSize; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
}
