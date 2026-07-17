package com.tradeforge.order.service;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.domain.OrderType;
import com.tradeforge.order.web.dto.OrderResponse;
import com.tradeforge.order.web.dto.PlaceOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order submission service")
class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderValidationService orderValidationService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        orderValidationService = Mockito.mock(OrderValidationService.class);
        orderService = new OrderService(orderRepository, orderValidationService);
    }

    @Test
    @DisplayName("happy path: valid order submission creates ACCEPTED order with sequence number")
    void submitOrder_validRequest_returnsAcceptedOrder() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Instrument mockInstrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(mockInstrument.getId()).thenReturn(instrumentId);

        PlaceOrderRequest request = new PlaceOrderRequest(
                "CID-001",
                "CAL",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100.00"),
                new BigDecimal("50")
        );

        // Mock validation to succeed
        Mockito.when(orderValidationService.validate(
                "CID-001", userId, "CAL", OrderSide.BUY, new BigDecimal("100.00"), new BigDecimal("50")
        )).thenReturn(mockInstrument);

        // Mock order persistence
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        Mockito.when(orderRepository.save(Mockito.any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    return order;
                });

        // Submit
        OrderResponse response = orderService.submitOrder(userId, request);

        // Assertions
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(response.clientOrderId()).isEqualTo("CID-001");
        assertThat(response.side()).isEqualTo(OrderSide.BUY);
        assertThat(response.limitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.originalQuantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(response.remainingQuantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(response.filledQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.sequenceNumber()).isGreaterThan(0L);

        // Verify persistence was called
        Mockito.verify(orderRepository).save(Mockito.any(Order.class));

        // Verify validation was called
        Mockito.verify(orderValidationService).validate(
                "CID-001", userId, "CAL", OrderSide.BUY, new BigDecimal("100.00"), new BigDecimal("50")
        );
    }

    @Test
    @DisplayName("duplicate client ID rejection propagates from validation service")
    void submitOrder_duplicateClientId_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        PlaceOrderRequest request = new PlaceOrderRequest(
                "CID-DUP",
                "CAL",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100.00"),
                new BigDecimal("50")
        );

        // Mock validation to throw
        Mockito.when(orderValidationService.validate(
                Mockito.anyString(), Mockito.any(), Mockito.anyString(),
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenThrow(new BusinessRuleException(
                ErrorCode.ORDER_CLIENT_ID_DUPLICATE,
                "Client order ID already exists"
        ));

        // Submit and expect exception
        assertThatThrownBy(() -> orderService.submitOrder(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_CLIENT_ID_DUPLICATE);

        // Verify no persistence
        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("market closed rejection propagates from validation service")
    void submitOrder_marketClosed_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        PlaceOrderRequest request = new PlaceOrderRequest(
                "CID-002",
                "CAL",
                OrderSide.BUY,
                OrderType.LIMIT,
                new BigDecimal("100.00"),
                new BigDecimal("50")
        );

        // Mock validation to throw market closed
        Mockito.when(orderValidationService.validate(
                Mockito.anyString(), Mockito.any(), Mockito.anyString(),
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenThrow(new BusinessRuleException(
                ErrorCode.ORDER_MARKET_CLOSED,
                "Orders can only be placed during market hours"
        ));

        // Submit and expect exception
        assertThatThrownBy(() -> orderService.submitOrder(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_MARKET_CLOSED);

        // Verify no persistence
        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("consecutive orders get incrementing sequence numbers")
    void submitOrder_multiple_sequenceNumbersIncrement() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Instrument mockInstrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(mockInstrument.getId()).thenReturn(instrumentId);

        PlaceOrderRequest request1 = new PlaceOrderRequest("CID-001", "CAL", OrderSide.BUY, OrderType.LIMIT, new BigDecimal("100.00"), new BigDecimal("10"));
        PlaceOrderRequest request2 = new PlaceOrderRequest("CID-002", "CAL", OrderSide.SELL, OrderType.LIMIT, new BigDecimal("101.00"), new BigDecimal("20"));

        Mockito.when(orderValidationService.validate(
                Mockito.anyString(), Mockito.any(), Mockito.anyString(),
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(mockInstrument);

        Mockito.when(orderRepository.save(Mockito.any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Submit first order
        OrderResponse response1 = orderService.submitOrder(userId, request1);
        long seq1 = response1.sequenceNumber();

        // Submit second order
        OrderResponse response2 = orderService.submitOrder(userId, request2);
        long seq2 = response2.sequenceNumber();

        // Verify sequence increments
        assertThat(seq2).isEqualTo(seq1 + 1);
    }
}
