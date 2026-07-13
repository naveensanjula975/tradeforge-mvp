package com.tradeforge.common.exception;

/**
 * Stable, machine-readable error codes returned in every API error response.
 *
 * <p>Adding a new code is safe; renaming an existing code is a breaking change
 * and requires a new API version.
 */
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────────────────────────
    INVALID_REQUEST,
    INTERNAL_SERVER_ERROR,
    RESOURCE_NOT_FOUND,
    CONFLICT,

    // ── Authentication / Authorisation ───────────────────────────────────────
    AUTHENTICATION_FAILED,
    ACCESS_DENIED,
    TOKEN_EXPIRED,
    TOKEN_INVALID,

    // ── User ─────────────────────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS,
    USER_NOT_FOUND,
    USER_INACTIVE,

    // ── Instrument ───────────────────────────────────────────────────────────
    INSTRUMENT_NOT_FOUND,
    INSTRUMENT_INACTIVE,
    INSTRUMENT_SYMBOL_DUPLICATE,

    // ── Order validation ──────────────────────────────────────────────────────
    ORDER_INVALID_PRICE,
    ORDER_INVALID_QUANTITY,
    ORDER_PRICE_NOT_ON_TICK,
    ORDER_QUANTITY_NOT_ON_LOT,
    ORDER_MARKET_CLOSED,
    ORDER_CLIENT_ID_DUPLICATE,
    ORDER_NOT_FOUND,

    // ── Order state ───────────────────────────────────────────────────────────
    ORDER_INVALID_TRANSITION,
    ORDER_ALREADY_TERMINAL,

    // ── Risk / Portfolio ─────────────────────────────────────────────────────
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_POSITION,
    ACCOUNT_NOT_FOUND
}
