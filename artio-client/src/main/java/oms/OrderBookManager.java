package oms;

import java.util.HashMap;
import java.util.Map;

public class OrderBookManager {

    private final Map<String, OrderBook> ccyPairToOrderBook;

    public OrderBookManager() {
        this.ccyPairToOrderBook = new HashMap<>();
    }

    public void addOrder(Order order) {
        ccyPairToOrderBook.computeIfAbsent(order.getCcyPair(), book -> new OrderBook())
                .addOrder(order.getOrderId(), order.getClientId(), order.getTargetPrice(), order.isBid());
    }
}
