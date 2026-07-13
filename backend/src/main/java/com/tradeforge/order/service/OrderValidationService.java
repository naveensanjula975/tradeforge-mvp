package com.tradeforge.order.service;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.service.InstrumentService;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.vo.Price;
import com.tradeforge.order.domain.vo.Quantity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Validates all business rules before an order is accepted into the matching engine.
 *
 * <p>Each rule produces a stable {@link ErrorCode}. Validation logic is independent
 * of controllers — it can be called from any entry point.
 *
 * <h3>Rules (in execution order)</h3>
 * <ol>
 *   <li>Price must be positive.</li>
 *   <li>Quantity must be positive.</li>
 *   <li>Price must align to instrument tick size.</li>
 *   <li>Quantity must align to instrument lot size.</li>
 *   <li>Instrument must be ACTIVE.</li>
 *   <li>Client order ID must not already exist for this user.</li>
 * </ol>
 *
 * <p>Market hours check (rule 7) and User inactive check (rule 8) are stubbed for
 * MVP and can be enabled in a later milestone.
 */
@Service
public class OrderValidationService {

    private final InstrumentService instrumentService;
    private final OrderRepository   orderRepository;

    public OrderValidationService(InstrumentService instrumentService, OrderRepository orderRepository) {
        this.instrumentService = instrumentService;
        this.orderRepository   = orderRepository;
    }

    /**
     * Full pre-acceptance validation of an incoming order.
     *
     * @param clientOrderId  caller-assigned unique ID (per user)
     * @param userId         authenticated user placing the order
     * @param symbol         instrument symbol (e.g. "CAL")
     * @param side           BUY or SELL
     * @param rawPrice       limit price as BigDecimal
     * @param rawQuantity    order quantity as BigDecimal
     * @return validated {@link Instrument} (needed for subsequent processing)
     * @throws BusinessRuleException with stable error code if any rule is violated
     */
    public Instrument validate(
            String  clientOrderId,
            UUID    userId,
            String  symbol,
            OrderSide side,
            BigDecimal rawPrice,
            BigDecimal rawQuantity) {

        // Rule 1: Positive price (value object factory enforces this)
        Price price = Price.of(rawPrice);

        // Rule 2: Positive quantity (value object factory enforces this)
        Quantity quantity = Quantity.of(rawQuantity);

        // Rule 5: Instrument must exist and be ACTIVE
        Instrument instrument = instrumentService.requireBySymbol(symbol);
        instrument.requireActive();

        // Rule 3: Price on tick
        price.requireOnTick(instrument.getTickSize());

        // Rule 4: Quantity on lot
        quantity.requireOnLot(instrument.getLotSize());

        // Rule 6: Duplicate client order ID per user
        if (orderRepository.existsByUserIdAndClientOrderId(userId, clientOrderId)) {
            throw new BusinessRuleException(ErrorCode.ORDER_CLIENT_ID_DUPLICATE,
                    "Client order ID '" + clientOrderId + "' has already been used.");
        }

        // Rule 7: Market hours — STUBBED (always open in MVP)
        // Rule 8: User inactive — STUBBED (handled by JWT auth)

        return instrument;
    }
}
