package com.tradeforge.order.web;

import com.tradeforge.order.domain.Order;
import com.tradeforge.order.service.matching.MatchingEngine;
import com.tradeforge.order.service.matching.OrderBook;
import com.tradeforge.order.web.dto.OrderBookResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MatchingEngine matchingEngine;

    public MarketController(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    @GetMapping("/{symbol}/orderbook")
    public OrderBookResponse getOrderBook(@PathVariable String symbol) {
        OrderBook book = matchingEngine.getOrderBook(symbol);
        if (book == null) {
            return new OrderBookResponse(symbol.toUpperCase(), Collections.emptyList(), Collections.emptyList(), Instant.now().toString());
        }

        List<OrderBookResponse.Level> bids = book.getBids().stream()
                .collect(Collectors.groupingBy(Order::getLimitPrice))
                .entrySet().stream()
                .map(entry -> new OrderBookResponse.Level(
                        entry.getKey().stripTrailingZeros().toPlainString(),
                        entry.getValue().stream().map(Order::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString(),
                        entry.getValue().size()
                ))
                .sorted((l1, l2) -> new BigDecimal(l2.price()).compareTo(new BigDecimal(l1.price())))
                .toList();

        List<OrderBookResponse.Level> asks = book.getAsks().stream()
                .collect(Collectors.groupingBy(Order::getLimitPrice))
                .entrySet().stream()
                .map(entry -> new OrderBookResponse.Level(
                        entry.getKey().stripTrailingZeros().toPlainString(),
                        entry.getValue().stream().map(Order::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString(),
                        entry.getValue().size()
                ))
                .sorted((l1, l2) -> new BigDecimal(l1.price()).compareTo(new BigDecimal(l2.price())))
                .toList();

        return new OrderBookResponse(symbol.toUpperCase(), bids, asks, Instant.now().toString());
    }

    @GetMapping("/{symbol}/trades")
    public org.springframework.data.domain.Page<Object> getPublicTrades(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Return an empty page of trades (since Trades are transient event-driven in MVP)
        return org.springframework.data.domain.Page.empty();
    }
}
