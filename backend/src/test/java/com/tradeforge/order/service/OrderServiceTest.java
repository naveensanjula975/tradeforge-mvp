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
import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order submission service")
class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderValidationService orderValidationService;
    private AccountRepository accountRepository;
    private PositionRepository positionRepository;
    private com.tradeforge.instrument.service.InstrumentService instrumentService;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        orderValidationService = Mockito.mock(OrderValidationService.class);
        accountRepository = Mockito.mock(AccountRepository.class);
        positionRepository = Mockito.mock(PositionRepository.class);
        instrumentService = Mockito.mock(com.tradeforge.instrument.service.InstrumentService.class);
        eventPublisher = Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);
        orderService = new OrderService(orderRepository, orderValidationService, accountRepository, positionRepository, instrumentService, eventPublisher);
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

        // Mock Account lookup for risk check
        Account account = Account.create(userId, new BigDecimal("10000.00"));
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

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

        Account account = Account.create(userId, new BigDecimal("10000.00"));
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        Mockito.when(positionRepository.findByAccountIdAndInstrumentId(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(Position.create(account.getId(), instrumentId, new BigDecimal("100"), new BigDecimal("10.00"))));

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

    @Test
    @DisplayName("getOrder returns order when it exists and belongs to user")
    void getOrder_existsAndBelongsToUser_returnsOrder() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Order order = Order.create("CID-123", userId, instrumentId, OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("100"));
        
        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId, userId);

        assertThat(response).isNotNull();
        assertThat(response.clientOrderId()).isEqualTo("CID-123");
    }

    @Test
    @DisplayName("getOrder throws ResourceNotFoundException when order does not exist or belongs to another user")
    void getOrder_notExistsOrDifferentUser_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId, userId))
                .isInstanceOf(com.tradeforge.common.exception.ResourceNotFoundException.class)
                .extracting(ex -> ((com.tradeforge.common.exception.ResourceNotFoundException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("getOrders returns paginated orders filtered by status when status is provided")
    void getOrders_withStatus_returnsFilteredOrders() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Order order = Order.create("CID-123", userId, instrumentId, OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("100"));
        order.accept();

        org.springframework.data.domain.Page<Order> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(order));
        Mockito.when(orderRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                Mockito.eq(userId), Mockito.eq(OrderStatus.ACCEPTED), Mockito.any(org.springframework.data.domain.PageRequest.class)
        )).thenReturn(page);

        org.springframework.data.domain.Page<OrderResponse> result = orderService.getOrders(userId, OrderStatus.ACCEPTED, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("getOrders returns all paginated orders when status is not provided")
    void getOrders_withoutStatus_returnsAllOrders() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Order order = Order.create("CID-123", userId, instrumentId, OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("100"));

        org.springframework.data.domain.Page<Order> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(order));
        Mockito.when(orderRepository.findAllByUserIdOrderByCreatedAtDesc(
                Mockito.eq(userId), Mockito.any(org.springframework.data.domain.PageRequest.class)
        )).thenReturn(page);

        org.springframework.data.domain.Page<OrderResponse> result = orderService.getOrders(userId, null, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("cancelOrder cancels accepted order and saves to database")
    void cancelOrder_validOrder_cancelsAndSaves() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Order order = Order.create("CID-123", userId, instrumentId, OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("100"));
        order.accept();

        Account account = Account.create(userId, new BigDecimal("10000.00"));
        account.reserveCash(new BigDecimal("1000.00")); // Reserve cash beforehand
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        Mockito.when(orderRepository.save(Mockito.any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, userId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(account.availableCash()).isEqualByComparingTo("10000.00"); // 1000 cash released, available returns to 10000
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder throws ResourceNotFoundException when order does not exist or belongs to another user")
    void cancelOrder_notExistsOrDifferentUser_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(com.tradeforge.common.exception.ResourceNotFoundException.class)
                .extracting(ex -> ((com.tradeforge.common.exception.ResourceNotFoundException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("cancelOrder throws BusinessRuleException when order is in terminal state")
    void cancelOrder_terminalState_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Order order = Order.create("CID-123", userId, instrumentId, OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("100"));
        order.reject("failed validation");

        Mockito.when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("submitOrder BUY rejects with INSUFFICIENT_FUNDS if available cash is too low")
    void submitOrder_buyInsufficientFunds_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Instrument mockInstrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(mockInstrument.getId()).thenReturn(instrumentId);

        PlaceOrderRequest request = new PlaceOrderRequest("CID-001", "CAL", OrderSide.BUY, OrderType.LIMIT, new BigDecimal("100.00"), new BigDecimal("50"));

        Mockito.when(orderValidationService.validate(
                Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(mockInstrument);

        // Account has only 1000 cash, order requires 100 * 50 = 5000
        Account account = Account.create(userId, new BigDecimal("1000.00"));
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> orderService.submitOrder(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }

    @Test
    @DisplayName("submitOrder SELL rejects with INSUFFICIENT_POSITION if position is too low")
    void submitOrder_sellInsufficientPosition_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        Instrument mockInstrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(mockInstrument.getId()).thenReturn(instrumentId);

        PlaceOrderRequest request = new PlaceOrderRequest("CID-001", "CAL", OrderSide.SELL, OrderType.LIMIT, new BigDecimal("100.00"), new BigDecimal("50"));

        Mockito.when(orderValidationService.validate(
                Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(mockInstrument);

        Account account = Account.create(userId, new BigDecimal("10000.00"));
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // Position has only 10 shares, order requires 50
        Position position = Position.create(account.getId(), instrumentId, new BigDecimal("10"), new BigDecimal("95.00"));
        Mockito.when(positionRepository.findByAccountIdAndInstrumentId(account.getId(), instrumentId)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> orderService.submitOrder(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_POSITION);
    }
}
