package com.tradeforge.order.service.matching;

import com.tradeforge.order.domain.Order;
import com.tradeforge.order.domain.OrderSide;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class OrderBook {

    private final String symbol;

    private final TreeSet<Order> bids = new TreeSet<>((o1, o2) -> {
        int priceCompare = o2.getLimitPrice().compareTo(o1.getLimitPrice()); // Descending
        if (priceCompare != 0) return priceCompare;
        int seqCompare = Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber()); // Ascending
        if (seqCompare != 0) return seqCompare;
        return o1.getId().compareTo(o2.getId());
    });

    private final TreeSet<Order> asks = new TreeSet<>((o1, o2) -> {
        int priceCompare = o1.getLimitPrice().compareTo(o2.getLimitPrice()); // Ascending
        if (priceCompare != 0) return priceCompare;
        int seqCompare = Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber()); // Ascending
        if (seqCompare != 0) return seqCompare;
        return o1.getId().compareTo(o2.getId());
    });

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public synchronized void add(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
        } else {
            asks.add(order);
        }
    }

    public synchronized void remove(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.remove(order);
        } else {
            asks.remove(order);
        }
    }

    public synchronized Order getBestOpposing(OrderSide side) {
        if (side == OrderSide.BUY) {
            return asks.isEmpty() ? null : asks.first();
        } else {
            return bids.isEmpty() ? null : bids.first();
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public synchronized Set<Order> getBids() {
        return Collections.unmodifiableSet(new TreeSet<>(bids));
    }

    public synchronized Set<Order> getAsks() {
        return Collections.unmodifiableSet(new TreeSet<>(asks));
    }
}
