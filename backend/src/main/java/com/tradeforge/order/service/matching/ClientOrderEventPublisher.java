package com.tradeforge.order.service.matching;

import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderStatus;
import com.tradeforge.order.domain.event.OrderAcceptedEvent;
import com.tradeforge.order.domain.event.OrderCancelledEvent;
import com.tradeforge.order.domain.event.TradeExecutionEvent;
import com.tradeforge.order.web.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
public class ClientOrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClientOrderEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;

    public ClientOrderEventPublisher(SimpMessagingTemplate messagingTemplate, OrderRepository orderRepository) {
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            WsOrderEventDto wsEvent = new WsOrderEventDto(
                    "ORDER_ACCEPTED",
                    OrderResponse.from(order),
                    null,
                    Instant.now().toString()
            );
            sendToUser(order.getUserId(), wsEvent);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            WsOrderEventDto wsEvent = new WsOrderEventDto(
                    "ORDER_CANCELLED",
                    OrderResponse.from(order),
                    null,
                    Instant.now().toString()
            );
            sendToUser(order.getUserId(), wsEvent);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTradeExecution(TradeExecutionEvent event) {
        // Notify buyer
        orderRepository.findById(event.buyOrderId()).ifPresent(order -> {
            String type = order.getStatus() == OrderStatus.FILLED ? "ORDER_FILLED" : "ORDER_PARTIALLY_FILLED";
            WsOrderEventDto wsEvent = new WsOrderEventDto(
                    type,
                    OrderResponse.from(order),
                    new TradeDto(
                            UUID.randomUUID().toString(),
                            event.symbol(),
                            "BUY",
                            event.price().stripTrailingZeros().toPlainString(),
                            event.quantity().stripTrailingZeros().toPlainString(),
                            event.timestamp().toString()
                    ),
                    event.timestamp().toString()
            );
            sendToUser(order.getUserId(), wsEvent);
        });

        // Notify seller
        orderRepository.findById(event.sellOrderId()).ifPresent(order -> {
            String type = order.getStatus() == OrderStatus.FILLED ? "ORDER_FILLED" : "ORDER_PARTIALLY_FILLED";
            WsOrderEventDto wsEvent = new WsOrderEventDto(
                    type,
                    OrderResponse.from(order),
                    new TradeDto(
                            UUID.randomUUID().toString(),
                            event.symbol(),
                            "SELL",
                            event.price().stripTrailingZeros().toPlainString(),
                            event.quantity().stripTrailingZeros().toPlainString(),
                            event.timestamp().toString()
                    ),
                    event.timestamp().toString()
            );
            sendToUser(order.getUserId(), wsEvent);
        });
    }

    private void sendToUser(UUID userId, WsOrderEventDto event) {
        log.debug("Sending order event [{}] to user [{}]", event.type(), userId);
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/orders", event);
    }

    // ── DTO Records matching frontend types ───────────────────────────────────

    public record TradeDto(
            String id,
            String symbol,
            String side,
            String executionPrice,
            String executionQuantity,
            String executedAt
    ) {}

    public record WsOrderEventDto(
            String type,
            OrderResponse order,
            TradeDto trade,
            String timestamp
    ) {}
}
