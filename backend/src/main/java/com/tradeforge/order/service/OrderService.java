package com.tradeforge.order.service;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.web.dto.OrderResponse;
import com.tradeforge.order.web.dto.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates order placement: validates, creates, accepts, and persists.
 *
 * <p>Sequence numbers are assigned atomically to ensure deterministic order book ordering.
 * In a production system with clustering, sequence numbers would use a distributed counter.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderValidationService orderValidationService;
    private final AtomicLong sequenceCounter = new AtomicLong(1);

    public OrderService(
            OrderRepository orderRepository,
            OrderValidationService orderValidationService) {
        this.orderRepository = orderRepository;
        this.orderValidationService = orderValidationService;
    }

    /**
     * Submit a new order for acceptance.
     *
     * <ol>
     *   <li>Validate all business rules via OrderValidationService.</li>
     *   <li>Create Order aggregate in PENDING_VALIDATION state.</li>
     *   <li>Transition to ACCEPTED and assign sequence number.</li>
     *   <li>Persist to database.</li>
     *   <li>Log submission.</li>
     *   <li>Return order response.</li>
     * </ol>
     *
     * @param userId authenticated trader placing the order
     * @param request validated PlaceOrderRequest
     * @return order with ACCEPTED status and assigned sequence number
     * @throws BusinessRuleException if any validation fails
     */
    @Transactional
    public OrderResponse submitOrder(UUID userId, PlaceOrderRequest request) {
        log.info("Order submission started: user={}, clientId={}, symbol={}, side={}, qty={}",
                userId, request.clientOrderId(), request.symbol(), request.side(), request.quantity());

        // Step 1: Validate all business rules
        Instrument instrument = orderValidationService.validate(
                request.clientOrderId(),
                userId,
                request.symbol(),
                request.side(),
                request.limitPrice(),
                request.quantity());

        // Step 2: Create order aggregate (starts in PENDING_VALIDATION)
        Order order = Order.create(
                request.clientOrderId(),
                userId,
                instrument.getId(),
                request.side(),
                request.limitPrice(),
                request.quantity());

        // Step 3: Accept order and assign sequence number
        order.accept();
        long sequenceNumber = sequenceCounter.getAndIncrement();
        order.assignSequenceNumber(sequenceNumber);

        // Step 4: Persist
        Order saved = orderRepository.save(order);

        log.info("Order accepted: id={}, clientId={}, seq={}", saved.getId(), saved.getClientOrderId(), sequenceNumber);

        // Step 5: Return response
        return OrderResponse.from(saved);
    }
}
