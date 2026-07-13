package com.tradeforge.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response format.
 *
 * <pre>{@code
 * {
 *   "timestamp": "2026-07-13T10:30:00Z",
 *   "status": 400,
 *   "code": "INVALID_REQUEST",
 *   "message": "The request is invalid.",
 *   "correlationId": "uuid",
 *   "fieldErrors": [...]   // present only for validation failures
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String correlationId,
        List<FieldError> fieldErrors
) {

    /**
     * Individual field-level validation error.
     */
    public record FieldError(
            String field,
            String message
    ) {}

    // ── Factory helpers ───────────────────────────────────────────────────────

    public static ApiErrorResponse of(int status, String code, String message, String correlationId) {
        return new ApiErrorResponse(Instant.now(), status, code, message, correlationId, null);
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            String code,
            String message,
            String correlationId,
            List<FieldError> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, code, message, correlationId, fieldErrors);
    }
}
