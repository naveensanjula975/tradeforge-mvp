package com.tradeforge.order.service.matching;

import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MatchingService matchingService;
    private final MarketDataPublisher marketDataPublisher;

    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, String> instrumentIdToSymbol = new ConcurrentHashMap<>();

    public MatchingEngine(
            OrderRepository orderRepository,
            InstrumentRepository instrumentRepository,
            MatchingService matchingService,
            MarketDataPublisher marketDataPublisher) {
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.matchingService = matchingService;
        this.marketDataPublisher = marketDataPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initOrderBooks() {
        log.info("Rebuilding order books from database...");

        // Load all active instruments to populate cache
        List<Instrument> instruments = instrumentRepository.findAll();
        for (Instrument instrument : instruments) {
            instrumentIdToSymbol.put(instrument.getId(), instrument.getSymbol());
            books.put(instrument.getSymbol(), new OrderBook(instrument.getSymbol()));
        }

        // Rebuild order book state by replaying active orders
        List<Order> activeOrders = orderRepository.findAllActiveOrderByInstrumentAndSequence();
        for (Order order : activeOrders) {
            String symbol = instrumentIdToSymbol.get(order.getInstrumentId());
            if (symbol != null) {
                OrderBook book = books.computeIfAbsent(symbol, OrderBook::new);
                book.add(order);
                log.debug("Loaded resting order into book: symbol={}, id={}, seq={}", symbol, order.getId(), order.getSequenceNumber());
            }
        }
        log.info("Order books rebuilt. Total active instruments: {}, Total active orders: {}", books.size(), activeOrders.size());
    }

    public synchronized void match(Order incoming) {
        String symbol = getSymbolForInstrument(incoming.getInstrumentId());
        if (symbol == null) {
            log.error("Unknown instrument ID in match: {}", incoming.getInstrumentId());
            return;
        }

        OrderBook book = books.computeIfAbsent(symbol, OrderBook::new);
        log.debug("Matching incoming order: id={}, side={}, price={}, qty={}", incoming.getId(), incoming.getSide(), incoming.getLimitPrice(), incoming.getRemainingQuantity());

        while (incoming.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            Order resting = book.getBestOpposing(incoming.getSide());
            if (resting == null) {
                break; // No opposing orders
            }

            boolean canMatch = incoming.getSide() == OrderSide.BUY
                    ? incoming.getLimitPrice().compareTo(resting.getLimitPrice()) >= 0
                    : incoming.getLimitPrice().compareTo(resting.getLimitPrice()) <= 0;

            if (!canMatch) {
                break; // Prices do not cross
            }

            // Execution price is the resting order's limit price
            BigDecimal matchPrice = resting.getLimitPrice();
            BigDecimal matchQty = incoming.getRemainingQuantity().min(resting.getRemainingQuantity());

            try {
                // Execute persistent fill transactionally
                matchingService.executeFill(incoming.getId(), resting.getId(), matchPrice, matchQty, symbol);

                // Apply updates in-memory to keep order book synced
                incoming.applyFill(matchQty);
                resting.applyFill(matchQty);

                if (resting.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    book.remove(resting);
                }
            } catch (Exception e) {
                log.error("Failed to execute matching fill transactionally for orders {} & {}", incoming.getId(), resting.getId(), e);
                break; // Stop matching loop to prevent state divergence
            }
        }

        // If incoming order remains unfilled, add it to the book
        if (incoming.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 && !incoming.getStatus().isTerminal()) {
            book.add(incoming);
        }

        // Broadcast live order book update
        marketDataPublisher.publishOrderBook(symbol, book);
    }

    public synchronized void cancel(Order order) {
        String symbol = getSymbolForInstrument(order.getInstrumentId());
        if (symbol != null) {
            OrderBook book = books.get(symbol);
            if (book != null) {
                book.remove(order);
                log.debug("Removed cancelled order from book: id={}, symbol={}", order.getId(), symbol);
                marketDataPublisher.publishOrderBook(symbol, book);
            }
        }
    }

    public OrderBook getOrderBook(String symbol) {
        return books.get(symbol.toUpperCase());
    }

    private String getSymbolForInstrument(java.util.UUID instrumentId) {
        return instrumentIdToSymbol.computeIfAbsent(instrumentId, id ->
            instrumentRepository.findById(id)
                    .map(Instrument::getSymbol)
                    .orElse(null)
        );
    }
}
