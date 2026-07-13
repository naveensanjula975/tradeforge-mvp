package com.tradeforge.common.web;

import com.tradeforge.common.exception.DomainException;
import com.tradeforge.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Central error handler for all REST controllers.
 *
 * <p>Rules:
 * <ul>
 *   <li>Stack traces are logged at server side but <strong>never</strong> returned to clients.</li>
 *   <li>Every response includes a {@code correlationId} for client-side tracing.</li>
 *   <li>Domain exceptions map 1-to-1 with their declared HTTP status and error code.</li>
 *   <li>Unexpected exceptions map to {@code 500 INTERNAL_SERVER_ERROR} with a generic message.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException ex, HttpServletRequest request) {

        log.warn("Domain exception [{}] at {}: {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiErrorResponse.of(
                        ex.getHttpStatus().value(),
                        ex.getErrorCode().name(),
                        ex.getMessage(),
                        correlationId()));
    }

    // ── Bean Validation (@Valid) ───────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.debug("Validation failure at {}: {} field error(s)", request.getRequestURI(), fieldErrors.size());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.withFieldErrors(
                        HttpStatus.BAD_REQUEST.value(),
                        ErrorCode.INVALID_REQUEST.name(),
                        "The request contains invalid fields.",
                        correlationId(),
                        fieldErrors));
    }

    // ── Spring Security ───────────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failure at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        ErrorCode.AUTHENTICATION_FAILED.name(),
                        "Authentication is required.",
                        correlationId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        ErrorCode.ACCESS_DENIED.name(),
                        "You do not have permission to perform this action.",
                        correlationId()));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        // Log the full stack trace at server side; return a generic message to the client
        log.error("Unexpected error at {}", request.getRequestURI(), ex);

        return ResponseEntity
                .internalServerError()
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        "An unexpected error occurred. Please try again later.",
                        correlationId()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
