package com.tradeforge.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.auth.web.dto.AuthResponse;
import com.tradeforge.auth.web.dto.RegisterRequest;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderType;
import com.tradeforge.order.web.dto.PlaceOrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class MarketHoursClosedIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.tradeforge.auth.domain.UserRepository userRepository;

    private Instrument instrument;
    private AuthResponse buyerAuth;

    @DynamicPropertySource
    static void marketHoursProperties(DynamicPropertyRegistry registry) {
        LocalTime now = LocalTime.now(ZoneId.of("UTC"));
        // Ensure now is outside [open, close] (e.g. market was open earlier and is now closed)
        LocalTime openTime = now.minusHours(3);
        LocalTime closeTime = now.minusHours(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        registry.add("app.market-hours.enabled", () -> "true");
        registry.add("app.market-hours.timezone", () -> "UTC");
        registry.add("app.market-hours.open", () -> openTime.format(formatter));
        registry.add("app.market-hours.close", () -> closeTime.format(formatter));
    }

    @BeforeEach
    void setUp() throws Exception {
        cleanup();

        // Create instrument
        instrument = Instrument.create("XYZ", "XYZ Corp", new BigDecimal("0.10"), new BigDecimal("10"));
        instrumentRepository.save(instrument);

        // Register user
        RegisterRequest buyerReg = new RegisterRequest("Buyer Closed", "buyerclosed@tradeforge.local", "Password@1234");
        MvcResult buyerRegResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerReg)))
                .andExpect(status().isCreated())
                .andReturn();

        buyerAuth = objectMapper.readValue(buyerRegResult.getResponse().getContentAsString(), AuthResponse.class);
        UUID buyerId = buyerAuth.userId();

        // Fund user
        Account buyerAccount = accountRepository.findByUserId(buyerId).orElseThrow();
        buyerAccount.creditCash(new BigDecimal("10000.00"));
        accountRepository.save(buyerAccount);
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
    @DisplayName("Market Closed: Placing order outside market hours should be rejected")
    void testPlaceOrderOutsideMarketHoursFails() throws Exception {
        PlaceOrderRequest buyOrderRequest = new PlaceOrderRequest(
                "BUY-CID-102",
                "XYZ",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100.00"),
                new BigDecimal("50")
        );

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + buyerAuth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isUnprocessableEntity()) // BusinessRuleException translates to 422 Unprocessable Entity
                .andExpect(jsonPath("$.errorCode").value("ORDER_MARKET_CLOSED"));
    }
}
