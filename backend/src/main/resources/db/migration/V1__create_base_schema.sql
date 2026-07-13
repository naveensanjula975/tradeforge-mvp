-- =============================================================================
-- V1__create_base_schema.sql
-- TradeForge MVP — Base Schema Foundation
-- Milestone M1 (Task M1.4)
-- =============================================================================

-- ─── Extensions ───────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- =============================================================================
-- USERS
-- =============================================================================
CREATE TABLE users (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(254) NOT NULL,
    password_hash VARCHAR(72) NOT NULL,   -- BCrypt output is 60 chars; 72 is safe
    role         VARCHAR(20)  NOT NULL DEFAULT 'TRADER',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_users          PRIMARY KEY (id),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('TRADER', 'ADMIN')),
    CONSTRAINT chk_users_email   CHECK (email = lower(trim(email)))
);

COMMENT ON TABLE  users               IS 'Registered traders and administrators.';
COMMENT ON COLUMN users.email         IS 'Normalised (lower-cased, trimmed) unique email.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash; never serialised to clients.';

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_email ON users (email);

-- =============================================================================
-- TRADING ACCOUNTS
-- =============================================================================
CREATE TABLE accounts (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID           NOT NULL,
    cash_balance   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reserved_cash  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_accounts             PRIMARY KEY (id),
    CONSTRAINT uq_accounts_user        UNIQUE (user_id),
    CONSTRAINT fk_accounts_user        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_accounts_cash_pos   CHECK (cash_balance   >= 0),
    CONSTRAINT chk_accounts_res_pos    CHECK (reserved_cash  >= 0),
    CONSTRAINT chk_accounts_res_le_bal CHECK (reserved_cash  <= cash_balance)
);

COMMENT ON TABLE  accounts              IS 'One-to-one cash wallet per trader.';
COMMENT ON COLUMN accounts.cash_balance IS 'Total cash held (including reserved).';
COMMENT ON COLUMN accounts.reserved_cash IS 'Locked for open BUY orders.';
COMMENT ON COLUMN accounts.version      IS 'Optimistic lock version.';

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_accounts_user_id ON accounts (user_id);

-- =============================================================================
-- INSTRUMENTS
-- =============================================================================
CREATE TABLE instruments (
    id          UUID           NOT NULL DEFAULT gen_random_uuid(),
    symbol      VARCHAR(20)    NOT NULL,
    name        VARCHAR(100)   NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    tick_size   NUMERIC(19, 8) NOT NULL,
    lot_size    NUMERIC(19, 8) NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_instruments          PRIMARY KEY (id),
    CONSTRAINT uq_instruments_symbol   UNIQUE (symbol),
    CONSTRAINT chk_instruments_status  CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_instruments_tick    CHECK (tick_size > 0),
    CONSTRAINT chk_instruments_lot     CHECK (lot_size  > 0)
);

COMMENT ON TABLE  instruments           IS 'Tradeable securities.';
COMMENT ON COLUMN instruments.symbol    IS 'Short ticker (e.g. CAL, JKH, COMB).';
COMMENT ON COLUMN instruments.tick_size IS 'Minimum price increment.';
COMMENT ON COLUMN instruments.lot_size  IS 'Minimum quantity increment.';

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_instruments_symbol ON instruments (symbol);
CREATE INDEX idx_instruments_status ON instruments (status);

-- =============================================================================
-- POSITIONS
-- =============================================================================
CREATE TABLE positions (
    id                UUID           NOT NULL DEFAULT gen_random_uuid(),
    account_id        UUID           NOT NULL,
    instrument_id     UUID           NOT NULL,
    quantity          NUMERIC(19, 8) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(19, 8) NOT NULL DEFAULT 0,
    average_price     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_positions               PRIMARY KEY (id),
    CONSTRAINT uq_positions_acct_instr    UNIQUE (account_id, instrument_id),
    CONSTRAINT fk_positions_account       FOREIGN KEY (account_id)    REFERENCES accounts    (id),
    CONSTRAINT fk_positions_instrument    FOREIGN KEY (instrument_id) REFERENCES instruments (id),
    CONSTRAINT chk_positions_qty_pos      CHECK (quantity          >= 0),
    CONSTRAINT chk_positions_res_pos      CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_positions_res_le_qty   CHECK (reserved_quantity <= quantity),
    CONSTRAINT chk_positions_avg_pos      CHECK (average_price     >= 0)
);

COMMENT ON TABLE  positions                  IS 'Per-account holdings per instrument.';
COMMENT ON COLUMN positions.reserved_quantity IS 'Locked for open SELL orders.';
COMMENT ON COLUMN positions.version           IS 'Optimistic lock version.';

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_positions_account_id    ON positions (account_id);
CREATE INDEX idx_positions_instrument_id ON positions (instrument_id);
