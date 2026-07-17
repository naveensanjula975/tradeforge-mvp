package com.tradeforge.order.service;

import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.service.InstrumentService;
import com.tradeforge.order.domain.OrderRepository;
import com.tradeforge.order.domain.OrderSide;
import com.tradeforge.order.domain.vo.Price;
import com.tradeforge.order.domain.vo.Quantity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * <p>Market hours check (rule 7) is configurable and enforced here. User inactive
 * handling is not modeled as a separate domain rule yet and remains covered by the
 * authentication layer.
 */
@Service
public class OrderValidationService {

    private final InstrumentService instrumentService;
    private final OrderRepository   orderRepository;
    private final Clock             clock;
    private final boolean           marketHoursEnabled;
    private final ZoneId            marketZoneId;
    private final LocalTime         marketOpenTime;
    private final LocalTime         marketCloseTime;

    public OrderValidationService(
            InstrumentService instrumentService,
            OrderRepository orderRepository,
            Clock clock,
            @Value("${app.market-hours.enabled:true}") boolean marketHoursEnabled,
            @Value("${app.market-hours.timezone:Asia/Colombo}") String marketZoneId,
            @Value("${app.market-hours.open:09:30}") String marketOpenTime,
            @Value("${app.market-hours.close:14:30}") String marketCloseTime) {
        this.instrumentService = instrumentService;
        this.orderRepository   = orderRepository;
        this.clock             = clock;
        this.marketHoursEnabled = marketHoursEnabled;
        this.marketZoneId      = ZoneId.of(marketZoneId);
        this.marketOpenTime    = LocalTime.parse(marketOpenTime);
        this.marketCloseTime   = LocalTime.parse(marketCloseTime);
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

        // Rule 7: Market hours
        requireMarketOpen();

        return instrument;
    }

    private void requireMarketOpen() {
        if (!marketHoursEnabled) {
            return;
        }

        ZonedDateTime marketNow = ZonedDateTime.ofInstant(Instant.now(clock), marketZoneId);
        DayOfWeek dayOfWeek = marketNow.getDayOfWeek();
        boolean weekday = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        boolean withinSession = !marketNow.toLocalTime().isBefore(marketOpenTime)
                && marketNow.toLocalTime().isBefore(marketCloseTime);

        if (!weekday || !withinSession) {
            throw new BusinessRuleException(ErrorCode.ORDER_MARKET_CLOSED,
                    "Orders can only be placed during market hours (" + marketOpenTime +
                            "-" + marketCloseTime + " " + marketZoneId + ").");
        }
    }
}
