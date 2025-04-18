package oms;

import org.agrona.collections.LongArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import static oms.TestUtils.*;

public class OrderBookTest {

    private static final int NUMBER_OF_ORDERS = 2000;


    @Test
    public void testOrderBook() {
        long[] latencies = new long[NUMBER_OF_ORDERS];
        OrderBook orderBook = new OrderBook();
        for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
            // Generate unique order data
            long orderId = i + 1000L; // Start from 1000
            long clientId = 5000L + (i % 100); // Cycle through some client IDs
            // Generate slightly varying rates to populate different levels
            double targetRate = generateRandomRate(1.35000);
            byte side = (byte) ((i % 2 == 0) ? 0 : 1); // Alternate sides
            long startTime = System.nanoTime();
            boolean addResult = orderBook.addOrder(orderId, clientId, targetRate, side);
            long endTime = System.nanoTime();
            latencies[i] = endTime - startTime;

            Assertions.assertTrue(addResult);
        }

        processAndPrintLatencies(latencies);

        // Try to match order
        LongArrayList matchedOrders = new LongArrayList();
        long startTime = System.nanoTime();
        orderBook.match(generateRandomRate(1.35), generateRandomRate(1.35), matchedOrders);
        long endTime = System.nanoTime();
        System.out.println("--- Matched Orders Result ---");
        System.out.println("-----------------------------------------------------");
        System.out.println("Matched time: " + (endTime - startTime) / NANO_PER_MICRO + "µs");
        System.out.println("Matched size: " + matchedOrders.size());
        System.out.println("-----------------------------------------------------");
    }




}
