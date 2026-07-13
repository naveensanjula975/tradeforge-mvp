package com.tradeforge.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all TradeForge domain exceptions.
 *
 * <p>Subclasses must provide a stable {@link ErrorCode} and an appropriate
 * HTTP status. The {@link com.tradeforge.common.web.GlobalExceptionHandler}
 * maps these to the standard API error response format.
 *
 * <p>Stack traces are caught but <strong>never</strong> returned to clients.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    protected DomainException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    protected DomainException(ErrorCode errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
