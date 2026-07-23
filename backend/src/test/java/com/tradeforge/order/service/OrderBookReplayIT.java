package com.tradeforge.order.service;

import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.service.matching.MatchingEngine;
import com.tradeforge.order.service.matching.OrderBook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.market-hours.enabled=false"
})
@ActiveProfiles("test")
public class OrderBookReplayIT {

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private com.tradeforge.auth.domain.UserRepository userRepository;

    @Autowired
    private com.tradeforge.account.domain.AccountRepository accountRepository;

    private Instrument instrument;

    @BeforeEach
    void setUp() {
        cleanup();

        // Create instrument
        instrument = Instrument.create("XYZ", "XYZ Corp", new BigDecimal("0.10"), new BigDecimal("10"));
        instrumentRepository.save(instrument);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        orderRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        instrumentRepository.deleteAll();
    }

    @Test
    @DisplayName("Order Book Replay: MatchingEngine correctly reconstructs OrderBook from DB active orders on init")
    void testOrderBookRebuiltFromDatabase() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        // Create two orders that are active (ACCEPTED)
        Order buyOrder = Order.create("BUY-REPLAY-01", buyerId, instrument.getId(), OrderSide.BUY, new BigDecimal("100.00"), new BigDecimal("50"));
        buyOrder.accept();
        buyOrder.assignSequenceNumber(10L);
        orderRepository.save(buyOrder);

        Order sellOrder = Order.create("SELL-REPLAY-01", sellerId, instrument.getId(), OrderSide.SELL, new BigDecimal("110.00"), new BigDecimal("30"));
        sellOrder.accept();
        sellOrder.assignSequenceNumber(11L);
        orderRepository.save(sellOrder);

        // Also create a filled order (which should NOT be replayed)
        Order filledOrder = Order.create("BUY-REPLAY-FILLED", buyerId, instrument.getId(), OrderSide.BUY, new BigDecimal("99.00"), new BigDecimal("10"));
        filledOrder.accept();
        filledOrder.assignSequenceNumber(12L);
        filledOrder.applyFill(new BigDecimal("10"));
        orderRepository.save(filledOrder);

        // Trigger rebuild
        matchingEngine.initOrderBooks();

        // Get the order book
        OrderBook book = matchingEngine.getOrderBook("XYZ");
        assertThat(book).isNotNull();

        // Assert bids has the buyOrder
        assertThat(book.getBids()).hasSize(1);
        Order replayedBid = book.getBids().iterator().next();
        assertThat(replayedBid.getClientOrderId()).isEqualTo("BUY-REPLAY-01");
        assertThat(replayedBid.getLimitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(replayedBid.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("50"));

        // Assert asks has the sellOrder
        assertThat(book.getAsks()).hasSize(1);
        Order replayedAsk = book.getAsks().iterator().next();
        assertThat(replayedAsk.getClientOrderId()).isEqualTo("SELL-REPLAY-01");
        assertThat(replayedAsk.getLimitPrice()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(replayedAsk.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("30"));
    }
}
