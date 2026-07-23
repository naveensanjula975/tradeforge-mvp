package com.tradeforge.order.service;

import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.auth.domain.User;
import com.tradeforge.auth.domain.UserRepository;
import com.tradeforge.auth.domain.UserRole;
import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.domain.OrderType;
import com.tradeforge.order.web.dto.OrderResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.market-hours.enabled=false"
})
@ActiveProfiles("test")
public class OrderEdgeCasesIT {

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
    private User buyer;
    private User seller;

    @BeforeEach
    void setUp() {
        cleanup();

        // Create instrument
        instrument = Instrument.create("XYZ", "XYZ Corp", new BigDecimal("0.10"), new BigDecimal("10"));
        instrumentRepository.save(instrument);

        // Create buyer
        buyer = User.create("Buyer Edge", "buyer_edge@tradeforge.local", passwordEncoder.encode("Password@1234"), UserRole.TRADER);
        userRepository.save(buyer);
        Account buyerAccount = Account.create(buyer.getId(), new BigDecimal("1000.00"));
        accountRepository.save(buyerAccount);

        // Create seller
        seller = User.create("Seller Edge", "seller_edge@tradeforge.local", passwordEncoder.encode("Password@1234"), UserRole.TRADER);
        userRepository.save(seller);
        Account sellerAccount = Account.create(seller.getId(), new BigDecimal("1000.00"));
        accountRepository.save(sellerAccount);
        Position sellerPos = Position.create(seller.getId(), instrument.getId(), new BigDecimal("100"), new BigDecimal("10.00"));
        positionRepository.save(sellerPos);
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
    @DisplayName("Idempotent Order Resubmission: Retrying same client order ID with matching params returns original order")
    void testIdempotentOrderSubmissionRetry() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                "IDEMPOTENT-CID-001",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("50")
        );

        OrderResponse firstResponse = orderService.submitOrder(buyer.getId(), request);
        assertThat(firstResponse.status()).isEqualTo(OrderStatus.ACCEPTED);

        // Resubmit identical order
        OrderResponse retryResponse = orderService.submitOrder(buyer.getId(), request);

        // Verify it returned the existing order
        assertThat(retryResponse.id()).isEqualTo(firstResponse.id());
        assertThat(retryResponse.sequenceNumber()).isEqualTo(firstResponse.sequenceNumber());
    }

    @Test
    @DisplayName("Duplicate Client Order ID Mismatch: Submitting existing client order ID with different params fails")
    void testDuplicateClientIdMismatchedParamsFails() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                "IDEMPOTENT-CID-002",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("50")
        );

        orderService.submitOrder(buyer.getId(), request);

        // Submit order with same client order ID but different quantity (60 vs 50)
        PlaceOrderRequest mismatchedRequest = new PlaceOrderRequest(
                "IDEMPOTENT-CID-002",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("60")
        );

        assertThatThrownBy(() -> orderService.submitOrder(buyer.getId(), mismatchedRequest))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CLIENT_ID_DUPLICATE);
    }

    @Test
    @DisplayName("Partial Fill Cancel: Cancelling partially filled order updates status and releases remaining quantity")
    void testCancelPartiallyFilledOrder() throws Exception {
        // Seller places 100 sell limit @ 10.00
        PlaceOrderRequest sellRequest = new PlaceOrderRequest(
                "SELL-PARTIAL-001",
                "XYZ",
                OrderSide.SELL,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("100")
        );
        OrderResponse sellResponse = orderService.submitOrder(seller.getId(), sellRequest);

        // Buyer places 40 buy limit @ 10.00 (triggers partial fill of 40 shares)
        PlaceOrderRequest buyRequest = new PlaceOrderRequest(
                "BUY-PARTIAL-001",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("40")
        );
        orderService.submitOrder(buyer.getId(), buyRequest);

        // Wait for async matching fill execution
        long endTime = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < endTime) {
            Order updatedSell = orderRepository.findById(sellResponse.id()).orElseThrow();
            if (updatedSell.getFilledQuantity().compareTo(new BigDecimal("40")) == 0) {
                break;
            }
            Thread.sleep(100);
        }

        Order partiallyFilledSell = orderRepository.findById(sellResponse.id()).orElseThrow();
        assertThat(partiallyFilledSell.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(partiallyFilledSell.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("60"));

        // Seller cancels remaining 60 shares
        OrderResponse cancelledResponse = orderService.cancelOrder(sellResponse.id(), seller.getId());
        assertThat(cancelledResponse.status()).isEqualTo(OrderStatus.CANCELLED);

        // Seller initial position was 100. Sold 40 -> position remaining in DB should be 60
        Position sellerPos = positionRepository.findByUserIdAndInstrumentId(seller.getId(), instrument.getId()).orElseThrow();
        assertThat(sellerPos.getQuantity()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(sellerPos.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Insufficient Funds: Submitting buy order costing more than cash balance is rejected")
    void testInsufficientFundsRejection() {
        // Buyer cash balance is 1000.00. Order cost is 2000.00 (200 @ 10.00)
        PlaceOrderRequest buyRequest = new PlaceOrderRequest(
                "BUY-EXCEED-CASH",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("10.00"),
                new BigDecimal("200")
        );

        assertThatThrownBy(() -> orderService.submitOrder(buyer.getId(), buyRequest))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }
}
