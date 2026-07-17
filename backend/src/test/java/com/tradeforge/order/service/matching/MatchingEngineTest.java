package com.tradeforge.order.service.matching;

import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Matching engine tests")
class MatchingEngineTest {

    private OrderRepository orderRepository;
    private InstrumentRepository instrumentRepository;
    private MatchingService matchingService;
    private MarketDataPublisher marketDataPublisher;
    private MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        instrumentRepository = Mockito.mock(InstrumentRepository.class);
        matchingService = Mockito.mock(MatchingService.class);
        marketDataPublisher = Mockito.mock(MarketDataPublisher.class);
        matchingEngine = new MatchingEngine(orderRepository, instrumentRepository, matchingService, marketDataPublisher);
    }

    @Test
    @DisplayName("initOrderBooks reloads active orders from database correctly")
    void initOrderBooks_replaysOrders() {
        UUID instrumentId = UUID.randomUUID();
        Instrument instrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(instrumentRepository.findAll()).thenReturn(List.of(instrument));
        Mockito.when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));

        Order order = Order.create("CID-100", UUID.randomUUID(), instrumentId, OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("50"));
        order.accept();
        order.assignSequenceNumber(1L);

        Mockito.when(orderRepository.findAllActiveOrderByInstrumentAndSequence()).thenReturn(List.of(order));

        matchingEngine.initOrderBooks();

        OrderBook book = matchingEngine.getOrderBook("CAL");
        assertThat(book).isNotNull();
        assertThat(book.getBids()).hasSize(1);
    }

    @Test
    @DisplayName("match executes fill transactionally and updates in-memory sizes")
    void match_crossesPrices_executesFill() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();

        Instrument instrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
        Mockito.when(instrumentRepository.findAll()).thenReturn(List.of(instrument));

        // Rebuild/Init books
        matchingEngine.initOrderBooks();

        Order restingSell = Order.create("CID-ASK", sellerId, instrumentId, OrderSide.SELL, new BigDecimal("10.00"), new BigDecimal("30"));
        restingSell.accept();
        restingSell.assignSequenceNumber(1L);

        // Add resting ask
        matchingEngine.match(restingSell);

        Order incomingBuy = Order.create("CID-BID", buyerId, instrumentId, OrderSide.BUY, new BigDecimal("10.50"), new BigDecimal("20"));
        incomingBuy.accept();
        incomingBuy.assignSequenceNumber(2L);

        // Match incoming bid
        matchingEngine.match(incomingBuy);

        // Verify match execution was called on matching service
        Mockito.verify(matchingService).executeFill(
                Mockito.eq(incomingBuy.getId()),
                Mockito.eq(restingSell.getId()),
                Mockito.eq(new BigDecimal("10.00")), // Resting price
                Mockito.eq(new BigDecimal("20")),    // Min of 20 and 30
                Mockito.eq("CAL")
        );

        // Verify remaining quantity in book
        assertThat(incomingBuy.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(restingSell.getRemainingQuantity()).isEqualByComparingTo("10");

        OrderBook book = matchingEngine.getOrderBook("CAL");
        assertThat(book.getBids()).isEmpty();
        assertThat(book.getAsks()).hasSize(1); // restingSell remains with 10 shares
    }
}
