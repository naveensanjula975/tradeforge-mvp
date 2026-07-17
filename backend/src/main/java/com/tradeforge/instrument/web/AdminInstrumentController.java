package com.tradeforge.instrument.web;

import com.tradeforge.instrument.domain.InstrumentStatus;
import com.tradeforge.instrument.service.InstrumentService;
import com.tradeforge.instrument.web.dto.CreateInstrumentRequest;
import com.tradeforge.instrument.web.dto.InstrumentResponse;
import com.tradeforge.instrument.web.dto.AdminStatsResponse;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.web.MarketController;
import com.tradeforge.order.web.dto.OrderBookResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/instruments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInstrumentController {

    private final InstrumentService instrumentService;
    private final OrderRepository orderRepository;
    private final MarketController marketController;

    public AdminInstrumentController(
            InstrumentService instrumentService,
            OrderRepository orderRepository,
            MarketController marketController) {
        this.instrumentService = instrumentService;
        this.orderRepository = orderRepository;
        this.marketController = marketController;
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

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                orderRepository.count(),
                orderRepository.sumAllFilledQuantity(),
                orderRepository.countDistinctUserIds()
        );
    }

    @GetMapping("/{symbol}/orderbook")
    public OrderBookResponse getOrderBook(@PathVariable String symbol) {
        return marketController.getOrderBook(symbol);
    }
}
