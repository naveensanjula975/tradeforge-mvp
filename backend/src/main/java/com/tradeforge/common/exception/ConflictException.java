package com.tradeforge.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule conflict is detected (e.g. duplicate email).
 * Maps to HTTP 409 Conflict.
 */
public class ConflictException extends DomainException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }
}
