package com.tradeforge.order.web.dto;

import java.util.List;

public record OrderBookResponse(
        String symbol,
        List<Level> bids,
        List<Level> asks,
        String timestamp
) {
    public record Level(String price, String quantity, int orderCount) {}
}
