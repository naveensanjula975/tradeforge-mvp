-- =============================================================================
-- V2__create_orders_table.sql
-- TradeForge MVP — Orders Schema
-- Milestone M3 (Task M3.5)
-- =============================================================================

CREATE TABLE orders (
    id                  UUID           NOT NULL DEFAULT gen_random_uuid(),
    client_order_id     VARCHAR(64)    NOT NULL,
    user_id             UUID           NOT NULL,
    instrument_id       UUID           NOT NULL,
    side                VARCHAR(10)    NOT NULL,
    type                VARCHAR(10)    NOT NULL DEFAULT 'LIMIT',
    limit_price         NUMERIC(19, 8) NOT NULL,
    original_quantity   NUMERIC(19, 8) NOT NULL,
    remaining_quantity  NUMERIC(19, 8) NOT NULL,
    filled_quantity     NUMERIC(19, 8) NOT NULL DEFAULT 0,
    status              VARCHAR(25)    NOT NULL DEFAULT 'PENDING_VALIDATION',
    sequence_number     BIGINT,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- ── Primary key ──────────────────────────────────────────────────────────
    CONSTRAINT pk_orders PRIMARY KEY (id),

    -- ── Unique client order ID per user ──────────────────────────────────────
    CONSTRAINT uq_orders_user_client_order_id UNIQUE (user_id, client_order_id),

    -- ── Foreign keys ─────────────────────────────────────────────────────────
    CONSTRAINT fk_orders_user       FOREIGN KEY (user_id)       REFERENCES users       (id),
    CONSTRAINT fk_orders_instrument FOREIGN KEY (instrument_id) REFERENCES instruments (id),

    -- ── Domain integrity constraints ─────────────────────────────────────────
    CONSTRAINT chk_orders_side         CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_orders_type         CHECK (type IN ('LIMIT')),
    CONSTRAINT chk_orders_status       CHECK (status IN (
                                          'PENDING_VALIDATION', 'ACCEPTED',
                                          'PARTIALLY_FILLED', 'FILLED',
                                          'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_orders_price_pos    CHECK (limit_price        > 0),
    CONSTRAINT chk_orders_orig_pos     CHECK (original_quantity  > 0),
    CONSTRAINT chk_orders_rem_nn       CHECK (remaining_quantity >= 0),
    CONSTRAINT chk_orders_fill_nn      CHECK (filled_quantity    >= 0),
    CONSTRAINT chk_orders_qty_balance  CHECK (
        filled_quantity + remaining_quantity = original_quantity
    )
);

COMMENT ON TABLE  orders                     IS 'Limit orders placed by traders.';
COMMENT ON COLUMN orders.client_order_id     IS 'Caller-assigned ID — unique per user.';
COMMENT ON COLUMN orders.sequence_number     IS 'Monotonic integer for price-time priority ordering.';
COMMENT ON COLUMN orders.version             IS 'Optimistic lock version.';

-- =============================================================================
-- INDEXES
-- =============================================================================

-- Active order lookup by instrument (order book queries + rebuild on startup)
CREATE INDEX idx_orders_instrument_status
    ON orders (instrument_id, status)
    WHERE status IN ('ACCEPTED', 'PARTIALLY_FILLED');

-- User order history
CREATE INDEX idx_orders_user_created
    ON orders (user_id, created_at DESC);

-- Sequence ordering for order book rebuild
CREATE INDEX idx_orders_instrument_sequence
    ON orders (instrument_id, sequence_number ASC)
    WHERE status IN ('ACCEPTED', 'PARTIALLY_FILLED');

-- Status-only queries (e.g. monitoring dashboards)
CREATE INDEX idx_orders_status ON orders (status);
