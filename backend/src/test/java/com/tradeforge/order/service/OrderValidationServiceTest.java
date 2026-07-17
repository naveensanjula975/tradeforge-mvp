package com.tradeforge.order.service;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.service.InstrumentService;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order validation — market hours")
class OrderValidationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private final InstrumentService instrumentService = Mockito.mock(InstrumentService.class);
    private final OrderRepository orderRepository = Mockito.mock(OrderRepository.class);

    private OrderValidationService newService(Clock clock) {
        return new OrderValidationService(
                instrumentService,
                orderRepository,
                clock,
                true,
                "Asia/Colombo",
                "09:30",
                "14:30");
    }

    @Test
    @DisplayName("accepts orders during the trading session")
    void validate_duringSession_returnsInstrument() {
        Instrument instrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(instrumentService.requireBySymbol("CAL")).thenReturn(instrument);
        Mockito.when(orderRepository.existsByUserIdAndClientOrderId(USER_ID, "CID-001")).thenReturn(false);

        Clock clock = Clock.fixed(Instant.parse("2026-07-17T04:00:00Z"), ZoneOffset.UTC);

        Instrument result = newService(clock).validate(
                "CID-001",
                USER_ID,
                "CAL",
                OrderSide.BUY,
                new BigDecimal("100.00"),
                new BigDecimal("10"));

        assertThat(result).isSameAs(instrument);
    }

    @Test
    @DisplayName("rejects orders before market open")
    void validate_beforeOpen_throwsMarketClosed() {
        Instrument instrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(instrumentService.requireBySymbol("CAL")).thenReturn(instrument);
        Mockito.when(orderRepository.existsByUserIdAndClientOrderId(USER_ID, "CID-002")).thenReturn(false);

        Clock clock = Clock.fixed(Instant.parse("2026-07-17T03:45:00Z"), ZoneOffset.UTC);

        assertThatThrownBy(() -> newService(clock).validate(
                "CID-002",
                USER_ID,
                "CAL",
                OrderSide.BUY,
                new BigDecimal("100.00"),
                new BigDecimal("10")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_MARKET_CLOSED);
    }
}