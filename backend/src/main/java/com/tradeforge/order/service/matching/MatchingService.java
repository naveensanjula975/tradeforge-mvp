package com.tradeforge.order.service.matching;

import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.common.exception.ResourceNotFoundException;
import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.event.TradeExecutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchingService(
            OrderRepository orderRepository,
            AccountRepository accountRepository,
            PositionRepository positionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executes a matched trade fill atomically in a new transaction.
     * This prevents lock contention and keeps matching latency low.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeFill(UUID incomingOrderId, UUID restingOrderId, BigDecimal matchPrice, BigDecimal matchQty, String symbol) {
        log.info("Executing fill: incoming={}, resting={}, price={}, qty={}", incomingOrderId, restingOrderId, matchPrice, matchQty);

        Order incoming = orderRepository.findById(incomingOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Incoming order not found: " + incomingOrderId));
        Order resting = orderRepository.findById(restingOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Resting order not found: " + restingOrderId));

        Order buyOrder = incoming.getSide() == OrderSide.BUY ? incoming : resting;
        Order sellOrder = incoming.getSide() == OrderSide.SELL ? incoming : resting;

        // Apply fills to order aggregates (updates remainingQty, filledQty, status)
        buyOrder.applyFill(matchQty);
        sellOrder.applyFill(matchQty);

        // Save updated orders
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        // Update BUY Account
        Account buyAccount = accountRepository.findByUserId(buyOrder.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Buyer account not found"));
        buyAccount.applyBuyExecution(matchPrice, matchQty, buyOrder.getLimitPrice());
        accountRepository.save(buyAccount);

        // Update SELL Account
        Account sellAccount = accountRepository.findByUserId(sellOrder.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Seller account not found"));
        sellAccount.applySellProceeds(matchPrice, matchQty);
        accountRepository.save(sellAccount);

        // Update BUY Position
        Position buyPosition = positionRepository.findByAccountIdAndInstrumentId(buyAccount.getId(), buyOrder.getInstrumentId())
                .orElseGet(() -> Position.create(buyAccount.getId(), buyOrder.getInstrumentId()));
        buyPosition.applyBuyExecution(matchQty, matchPrice);
        positionRepository.save(buyPosition);

        // Update SELL Position
        Position sellPosition = positionRepository.findByAccountIdAndInstrumentId(sellAccount.getId(), sellOrder.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INSUFFICIENT_POSITION, "Seller position not found"));
        sellPosition.applySellExecution(matchQty);
        positionRepository.save(sellPosition);

        // Emit TradeExecutionEvent
        eventPublisher.publishEvent(new TradeExecutionEvent(
                buyOrder.getId(),
                sellOrder.getId(),
                buyOrder.getUserId(),
                sellOrder.getUserId(),
                symbol,
                matchPrice,
                matchQty,
                Instant.now()
        ));

        log.info("Execution complete: buyOrder={}, sellOrder={}, fillQty={}", buyOrder.getId(), sellOrder.getId(), matchQty);
    }
}
