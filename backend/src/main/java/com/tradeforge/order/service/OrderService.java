package com.tradeforge.order.service;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.web.dto.OrderResponse;
import com.tradeforge.order.web.dto.PlaceOrderRequest;
import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.common.exception.ResourceNotFoundException;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.domain.event.OrderAcceptedEvent;
import com.tradeforge.order.domain.event.OrderCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates order placement: validates, creates, accepts, and persists.
 *
 * <p>Sequence numbers are assigned atomically to ensure deterministic order book ordering.
 * In a production system with clustering, sequence numbers would use a distributed counter.
 */
import com.tradeforge.instrument.service.InstrumentService;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderValidationService orderValidationService;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final InstrumentService instrumentService;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicLong sequenceCounter = new AtomicLong(1);

    public OrderService(
            OrderRepository orderRepository,
            OrderValidationService orderValidationService,
            AccountRepository accountRepository,
            PositionRepository positionRepository,
            InstrumentService instrumentService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderValidationService = orderValidationService;
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.instrumentService = instrumentService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Submit a new order for acceptance.
     */
    @Transactional
    public OrderResponse submitOrder(UUID userId, PlaceOrderRequest request) {
        log.info("Order submission started: user={}, clientId={}, symbol={}, side={}, qty={}",
                userId, request.clientOrderId(), request.symbol(), request.side(), request.quantity());

        // Step 0: Check for duplicate client order ID (Idempotency Check)
        Optional<Order> existingOpt = orderRepository.findByUserIdAndClientOrderId(userId, request.clientOrderId());
        if (existingOpt.isPresent()) {
            Order existing = existingOpt.get();
            Instrument inst = instrumentService.requireBySymbol(request.symbol());
            boolean matches = existing.getInstrumentId().equals(inst.getId())
                    && existing.getSide() == request.side()
                    && existing.getLimitPrice().compareTo(request.limitPrice()) == 0
                    && existing.getOriginalQuantity().compareTo(request.quantity()) == 0;
            if (matches) {
                log.info("Idempotent order submission retry detected for clientOrderId={}", request.clientOrderId());
                return OrderResponse.from(existing);
            } else {
                throw new BusinessRuleException(ErrorCode.ORDER_CLIENT_ID_DUPLICATE,
                        "Client order ID '" + request.clientOrderId() + "' has already been used with different order parameters.");
            }
        }

        // Step 1: Validate all business rules
        Instrument instrument = orderValidationService.validate(
                request.clientOrderId(),
                userId,
                request.symbol(),
                request.side(),
                request.limitPrice(),
                request.quantity());

        // Fetch User's Account (Risk Check boundary)
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found for user ID: " + userId));

        // Step 2: Risk Check & Reservation
        if (request.side() == OrderSide.BUY) {
            BigDecimal cost = request.limitPrice().multiply(request.quantity());
            account.reserveCash(cost);
            accountRepository.save(account);
        } else {
            Position position = positionRepository.findByAccountIdAndInstrumentId(account.getId(), instrument.getId())
                    .orElseGet(() -> Position.create(account.getId(), instrument.getId()));
            position.reserveQuantity(request.quantity());
            positionRepository.save(position);
        }

        // Step 3: Create order aggregate (starts in PENDING_VALIDATION)
        Order order = Order.create(
                request.clientOrderId(),
                userId,
                instrument.getId(),
                request.side(),
                request.limitPrice(),
                request.quantity());

        // Step 4: Accept order and assign sequence number
        order.accept();
        long sequenceNumber = sequenceCounter.getAndIncrement();
        order.assignSequenceNumber(sequenceNumber);

        // Step 5: Persist
        Order saved = orderRepository.save(order);

        log.info("Order accepted: id={}, clientId={}, seq={}", saved.getId(), saved.getClientOrderId(), sequenceNumber);

        // Publish event for matching (runs AFTER transaction commits)
        String correlationId = org.slf4j.MDC.get(com.tradeforge.common.web.CorrelationIdFilter.MDC_KEY);
        eventPublisher.publishEvent(new OrderAcceptedEvent(saved.getId(), correlationId));

        // Step 6: Return response
        return OrderResponse.from(saved);
    }

    /**
     * Retrieve a single order by ID and user ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND,
                        "Order not found for ID: " + id));
        return OrderResponse.from(order);
    }

    /**
     * Retrieve a paginated list of orders for a user, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(UUID userId, OrderStatus status, int page, int size) {
        if (size <= 0) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<Order> ordersPage;
        if (status != null) {
            ordersPage = orderRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageRequest);
        } else {
            ordersPage = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        }
        return ordersPage.map(OrderResponse::from);
    }

    /**
     * Cancel an active order.
     */
    @Transactional
    public OrderResponse cancelOrder(UUID id, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND,
                        "Order not found for ID: " + id));

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found for user ID: " + userId));

        // Release reserved funds/holdings
        if (order.getSide() == OrderSide.BUY) {
            BigDecimal reservedAmount = order.getRemainingQuantity().multiply(order.getLimitPrice());
            account.releaseCash(reservedAmount);
            accountRepository.save(account);
        } else {
            Position position = positionRepository.findByAccountIdAndInstrumentId(account.getId(), order.getInstrumentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.INSUFFICIENT_POSITION,
                            "Position not found for instrument ID: " + order.getInstrumentId()));
            position.releaseQuantity(order.getRemainingQuantity());
            positionRepository.save(position);
        }

        order.cancel();
        Order saved = orderRepository.save(order);
        log.info("Order cancelled: id={}, user={}", saved.getId(), userId);

        // Publish event for order book removal (runs AFTER transaction commits)
        String correlationId = org.slf4j.MDC.get(com.tradeforge.common.web.CorrelationIdFilter.MDC_KEY);
        eventPublisher.publishEvent(new OrderCancelledEvent(saved.getId(), correlationId));

        return OrderResponse.from(saved);
    }
}
