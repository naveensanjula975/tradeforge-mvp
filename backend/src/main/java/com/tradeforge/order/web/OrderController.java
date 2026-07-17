package com.tradeforge.order.web;

import com.tradeforge.order.service.OrderService;
import com.tradeforge.order.web.dto.OrderResponse;
import com.tradeforge.order.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Order management endpoints.
 *
 * <pre>
 * POST /api/v1/orders — place a new limit order (authenticated)
 * GET  /api/v1/orders/me — list my orders (paginated)
 * GET  /api/v1/orders/{id} — get order by ID (own orders only)
 * DELETE /api/v1/orders/{id} — cancel order (own orders only)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Submit a new limit order.
     *
     * @param request validated place order request
     * @param authentication Spring Security context (extracts user ID)
     * @return newly accepted order with ACCEPTED status
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return orderService.submitOrder(userId, request);
    }
}
