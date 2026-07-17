package com.tradeforge.order.service.matching;

import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.event.TradeExecutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MarketDataPublisher {

    private static final Logger log = LoggerFactory.getLogger(MarketDataPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public MarketDataPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts order book updates when matching engine state changes.
     */
    public void publishOrderBook(String symbol, OrderBook book) {
        List<OrderBookLevelDto> bids = book.getBids().stream()
                .collect(Collectors.groupingBy(Order::getLimitPrice))
                .entrySet().stream()
                .map(entry -> new OrderBookLevelDto(
                        entry.getKey().stripTrailingZeros().toPlainString(),
                        entry.getValue().stream().map(Order::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString(),
                        entry.getValue().size()
                ))
                .sorted((l1, l2) -> new BigDecimal(l2.price()).compareTo(new BigDecimal(l1.price()))) // Descending
                .toList();

        List<OrderBookLevelDto> asks = book.getAsks().stream()
                .collect(Collectors.groupingBy(Order::getLimitPrice))
                .entrySet().stream()
                .map(entry -> new OrderBookLevelDto(
                        entry.getKey().stripTrailingZeros().toPlainString(),
                        entry.getValue().stream().map(Order::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString(),
                        entry.getValue().size()
                ))
                .sorted((l1, l2) -> new BigDecimal(l1.price()).compareTo(new BigDecimal(l2.price()))) // Ascending
                .toList();

        OrderBookDto payload = new OrderBookDto(symbol, bids, asks, Instant.now().toString());
        WsMarketEventDto event = new WsMarketEventDto("ORDER_BOOK_UPDATE", symbol, payload, null, Instant.now().toString());

        log.debug("Publishing order book update for {}: bids={}, asks={}", symbol, bids.size(), asks.size());
        messagingTemplate.convertAndSend("/topic/market/" + symbol, event);
    }

    /**
     * Broadcasts a trade tick when a match completes.
     */
    @EventListener
    public void handleTradeExecution(TradeExecutionEvent event) {
        TradeDto trade = new TradeDto(
                UUID.randomUUID().toString(),
                event.symbol(),
                // Side is default to BUY/SELL of match, we can just use BUY as default or empty, let's use BUY
                "BUY", 
                event.price().stripTrailingZeros().toPlainString(),
                event.quantity().stripTrailingZeros().toPlainString(),
                event.timestamp().toString()
        );

        WsMarketEventDto wsEvent = new WsMarketEventDto(
                "TRADE_EXECUTED",
                event.symbol(),
                null,
                trade,
                event.timestamp().toString()
        );

        log.debug("Publishing trade tick for {}: price={}, qty={}", event.symbol(), event.price(), event.quantity());
        messagingTemplate.convertAndSend("/topic/market/" + event.symbol(), wsEvent);
    }

    // ── DTO Records for JSON serialization ────────────────────────────────────

    public record OrderBookLevelDto(String price, String quantity, int orderCount) {}

    public record OrderBookDto(String symbol, List<OrderBookLevelDto> bids, List<OrderBookLevelDto> asks, String timestamp) {}

    public record TradeDto(String id, String symbol, String side, String executionPrice, String executionQuantity, String executedAt) {}

    public record WsMarketEventDto(String type, String symbol, OrderBookDto orderBook, TradeDto trade, String timestamp) {}
}
