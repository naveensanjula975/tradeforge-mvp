package com.tradeforge.instrument.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/v1/admin/instruments}.
 */
public record CreateInstrumentRequest(

        @NotBlank(message = "Symbol is required.")
        @Size(min = 1, max = 20, message = "Symbol must be 1–20 characters.")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol must contain only uppercase letters and digits.")
        String symbol,

        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name must not exceed 100 characters.")
        String name,

        @NotNull(message = "Tick size is required.")
        @DecimalMin(value = "0.00000001", message = "Tick size must be positive.")
        BigDecimal tickSize,

        @NotNull(message = "Lot size is required.")
        @DecimalMin(value = "0.00000001", message = "Lot size must be positive.")
        BigDecimal lotSize
) {}
