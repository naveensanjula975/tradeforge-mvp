package com.tradeforge.instrument.web;

import com.tradeforge.instrument.domain.InstrumentStatus;
import com.tradeforge.instrument.service.InstrumentService;
import com.tradeforge.instrument.web.dto.CreateInstrumentRequest;
import com.tradeforge.instrument.web.dto.InstrumentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only instrument management endpoints.
 *
 * <pre>
 * POST  /api/v1/admin/instruments                  — create instrument
 * PATCH /api/v1/admin/instruments/{symbol}/status  — activate / deactivate
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/admin/instruments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInstrumentController {

    private final InstrumentService instrumentService;

    public AdminInstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(@Valid @RequestBody CreateInstrumentRequest request) {
        return instrumentService.create(request);
    }

    @PatchMapping("/{symbol}/status")
    public InstrumentResponse updateStatus(
            @PathVariable String symbol,
            @RequestParam InstrumentStatus status) {
        return instrumentService.updateStatus(symbol, status);
    }
}
