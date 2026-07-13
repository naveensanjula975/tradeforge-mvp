package com.tradeforge.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an order or domain operation violates a business rule.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
