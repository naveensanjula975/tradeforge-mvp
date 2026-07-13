package com.tradeforge.instrument.web;

import com.tradeforge.instrument.service.InstrumentService;
import com.tradeforge.instrument.web.dto.InstrumentResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public (authenticated) instrument endpoints.
 *
 * <pre>
 * GET /api/v1/instruments          — list all instruments
 * GET /api/v1/instruments/{symbol} — get single instrument
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public List<InstrumentResponse> listAll() {
        return instrumentService.listAll();
    }

    @GetMapping("/{symbol}")
    public InstrumentResponse getBySymbol(@PathVariable String symbol) {
        return instrumentService.getBySymbol(symbol);
    }
}
