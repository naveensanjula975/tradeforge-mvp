package com.tradeforge.order.service.matching;

import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.event.OrderAcceptedEvent;
import com.tradeforge.order.domain.event.OrderCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final MatchingEngine matchingEngine;
    private final OrderRepository orderRepository;

    public OrderEventListener(MatchingEngine matchingEngine, OrderRepository orderRepository) {
        this.matchingEngine = matchingEngine;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        log.debug("Received OrderAcceptedEvent for matching: {}", event.orderId());
        orderRepository.findById(event.orderId()).ifPresentOrElse(
                matchingEngine::match,
                () -> log.error("Order not found for matching: {}", event.orderId())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.debug("Received OrderCancelledEvent for book removal: {}", event.orderId());
        orderRepository.findById(event.orderId()).ifPresentOrElse(
                matchingEngine::cancel,
                () -> log.error("Order not found for book removal: {}", event.orderId())
        );
    }
}
