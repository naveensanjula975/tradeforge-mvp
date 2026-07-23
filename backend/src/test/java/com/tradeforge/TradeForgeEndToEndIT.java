package com.tradeforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.auth.domain.User;
import com.tradeforge.auth.domain.UserRepository;
import com.tradeforge.auth.domain.UserRole;
import com.tradeforge.auth.web.dto.AuthResponse;
import com.tradeforge.auth.web.dto.RegisterRequest;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.market-hours.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TradeForgeEndToEndIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @BeforeEach
    void setUp() {
        cleanup();
        // Create an instrument
        instrument = Instrument.create("XYZ", "XYZ Corp", new BigDecimal("0.10"), new BigDecimal("10"));
        instrumentRepository.save(instrument);
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
    @DisplayName("End-to-End Trade Flow: register -> login -> place matching orders -> verify fills and accounts")
    void testEndToEndTradeFlow() throws Exception {
        // 1. Register buyer and seller
        RegisterRequest buyerReg = new RegisterRequest("Buyer User", "buyer@tradeforge.local", "Password@1234");
        RegisterRequest sellerReg = new RegisterRequest("Seller User", "seller@tradeforge.local", "Password@1234");

        MvcResult buyerRegResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerReg)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse buyerAuth = objectMapper.readValue(buyerRegResult.getResponse().getContentAsString(), AuthResponse.class);
        UUID buyerId = buyerAuth.userId();

        MvcResult sellerRegResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerReg)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse sellerAuth = objectMapper.readValue(sellerRegResult.getResponse().getContentAsString(), AuthResponse.class);
        UUID sellerId = sellerAuth.userId();

        // 2. Fund accounts and set up starting positions
        // Buyer needs cash:
        Account buyerAccount = accountRepository.findByUserId(buyerId).orElseThrow();
        buyerAccount.creditCash(new BigDecimal("10000.00"));
        accountRepository.save(buyerAccount);

        // Seller needs position to sell:
        Account sellerAccount = accountRepository.findByUserId(sellerId).orElseThrow();
        // Give seller some cash as well
        sellerAccount.creditCash(new BigDecimal("5000.00"));
        accountRepository.save(sellerAccount);

        Position sellerPosition = Position.create(sellerId, instrument.getId(), new BigDecimal("100"), new BigDecimal("50.00"));
        positionRepository.save(sellerPosition);

        // 3. Seller places a Sell Limit Order for 50 qty @ 100.00
        PlaceOrderRequest sellOrderRequest = new PlaceOrderRequest(
                "SELL-CID-001",
                "XYZ",
                OrderSide.SELL,
                OrderType.LIMIT,
                new BigDecimal("100.00"),
                new BigDecimal("50")
        );

        MvcResult sellResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + sellerAuth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse sellResponse = objectMapper.readValue(sellResult.getResponse().getContentAsString(), OrderResponse.class);
        assertThat(sellResponse.status()).isEqualTo(OrderStatus.ACCEPTED);

        // 4. Buyer places a Buy Limit Order for 50 qty @ 100.00
        PlaceOrderRequest buyOrderRequest = new PlaceOrderRequest(
                "BUY-CID-001",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100.00"),
                new BigDecimal("50")
        );

        MvcResult buyResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + buyerAuth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse buyResponse = objectMapper.readValue(buyResult.getResponse().getContentAsString(), OrderResponse.class);
        assertThat(buyResponse.status()).isEqualTo(OrderStatus.FILLED);

        // 5. Verify seller's order is also filled now
        Order updatedSellOrder = orderRepository.findById(sellResponse.id()).orElseThrow();
        assertThat(updatedSellOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(updatedSellOrder.getFilledQuantity()).isEqualByComparingTo(new BigDecimal("50"));

        // 6. Verify buyer's account balance and position
        Account updatedBuyerAccount = accountRepository.findByUserId(buyerId).orElseThrow();
        // Initial cash was 10000.00. Cost of buy is 50 * 100 = 5000.00.
        assertThat(updatedBuyerAccount.getCashBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));

        List<Position> buyerPositions = positionRepository.findAllByUserId(buyerId);
        assertThat(buyerPositions).hasSize(1);
        assertThat(buyerPositions.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(buyerPositions.get(0).getAveragePrice()).isEqualByComparingTo(new BigDecimal("100.00"));

        // 7. Verify seller's account balance and position
        Account updatedSellerAccount = accountRepository.findByUserId(sellerId).orElseThrow();
        // Initial cash was 5000.00. Credit of sell is 5000.00.
        assertThat(updatedSellerAccount.getCashBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));

        Position updatedSellerPosition = positionRepository.findByUserIdAndInstrumentId(sellerId, instrument.getId()).orElseThrow();
        // Initial position was 100, sold 50 -> remaining 50
        assertThat(updatedSellerPosition.getQuantity()).isEqualByComparingTo(new BigDecimal("50"));
    }
}
