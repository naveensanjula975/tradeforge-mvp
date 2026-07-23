package com.tradeforge.order.service;

import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.auth.domain.User;
import com.tradeforge.auth.domain.UserRepository;
import com.tradeforge.auth.domain.UserRole;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.domain.OrderType;
import com.tradeforge.order.web.dto.PlaceOrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.market-hours.enabled=false"
})
@ActiveProfiles("test")
public class ConcurrentOrderSubmissionIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Instrument instrument;
    private User seller;
    private List<User> buyers;

    @BeforeEach
    void setUp() {
        cleanup();

        // Create instrument
        instrument = Instrument.create("XYZ", "XYZ Corp", new BigDecimal("0.10"), new BigDecimal("10"));
        instrumentRepository.save(instrument);

        // Create seller
        seller = User.create("Seller Concurrent", "seller_cc@tradeforge.local", passwordEncoder.encode("Password@1234"), UserRole.TRADER);
        userRepository.save(seller);
        Account sellerAccount = Account.create(seller.getId(), new BigDecimal("1000.00"));
        accountRepository.save(sellerAccount);
        Position sellerPos = Position.create(seller.getId(), instrument.getId(), new BigDecimal("1000"), new BigDecimal("10.00"));
        positionRepository.save(sellerPos);

        // Create 10 buyers
        buyers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User buyer = User.create("Buyer " + i, "buyer_" + i + "@tradeforge.local", passwordEncoder.encode("Password@1234"), UserRole.TRADER);
            userRepository.save(buyer);
            Account buyerAccount = Account.create(buyer.getId(), new BigDecimal("5000.00"));
            accountRepository.save(buyerAccount);
            buyers.add(buyer);
        }
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        orderRepository.deleteAll();
        positionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        instrumentRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent submissions: 10 buyers place orders simultaneously against 1 large resting sell order")
    void testConcurrentOrderSubmissions() throws Exception {
        // Place resting seller order: 1000 qty @ 10.00
        PlaceOrderRequest sellRequest = new PlaceOrderRequest(
                "SELL-CID-CC",
                "XYZ",
                OrderSide.SELL,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("1000")
        );
        var sellResponse = orderService.submitOrder(seller.getId(), sellRequest);
        assertThat(sellResponse.status()).isEqualTo(OrderStatus.ACCEPTED);

        // Set up concurrent buy submissions
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final User buyer = buyers.get(i);
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PlaceOrderRequest buyRequest = new PlaceOrderRequest(
                            "BUY-CID-CC-" + index,
                            "XYZ",
                            OrderSide.BUY,
                            OrderType.LIMIT,
                            new BigDecimal("10.00"),
                            new BigDecimal("100")
                    );
                    orderService.submitOrder(buyer.getId(), buyRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Start them all at once
        startLatch.countDown();

        // Wait for them to finish
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        // Allow some time for async event execution (afterCommit event listener runs async matching execution)
        // Wait up to 3 seconds for matching engine to execute all fills
        long endTime = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < endTime) {
            Order sellerOrder = orderRepository.findById(sellResponse.id()).orElseThrow();
            if (sellerOrder.getStatus() == OrderStatus.FILLED) {
                break;
            }
            Thread.sleep(100);
        }

        // Verify seller's order is FILLED
        Order finalSellerOrder = orderRepository.findById(sellResponse.id()).orElseThrow();
        assertThat(finalSellerOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(finalSellerOrder.getFilledQuantity()).isEqualByComparingTo(new BigDecimal("1000"));

        // Verify all 10 buyers' positions and balances
        for (User buyer : buyers) {
            Account buyerAccount = accountRepository.findByUserId(buyer.getId()).orElseThrow();
            // Initial cash 5000. Bought 100 @ 10.00 = 1000. Remaining cash should be 4000.00
            assertThat(buyerAccount.getCashBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));

            Position buyerPos = positionRepository.findByUserIdAndInstrumentId(buyer.getId(), instrument.getId()).orElseThrow();
            assertThat(buyerPos.getQuantity()).isEqualByComparingTo(new BigDecimal("100"));
        }

        // Verify seller position and balance
        Account finalSellerAccount = accountRepository.findByUserId(seller.getId()).orElseThrow();
        // Initial cash 1000. Credit of 10000. Total should be 11000.00
        assertThat(finalSellerAccount.getCashBalance()).isEqualByComparingTo(new BigDecimal("11000.00"));

        Position finalSellerPos = positionRepository.findByUserIdAndInstrumentId(seller.getId(), instrument.getId()).orElseThrow();
        assertThat(finalSellerPos.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
