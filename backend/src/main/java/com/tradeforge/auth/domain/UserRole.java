package com.tradeforge.auth.domain;

/**
 * User roles in the TradeForge system.
 */
public enum UserRole {

    /** Standard trader — can place and cancel orders, view their portfolio. */
    TRADER,

    /** Administrator — can create/disable instruments, manage system settings. */
    ADMIN
}
