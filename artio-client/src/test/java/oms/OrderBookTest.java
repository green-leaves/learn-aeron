package oms;

import org.agrona.collections.LongArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;

import static oms.TestUtils.generateRandomRate;

public class OrderBookTest {

    private static final int NUMBER_OF_ORDERS = 2000;
    private static final long NANO_PER_MICRO = 1000L;
    private static final long NANO_PER_MILLI = 1_000_000L;

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


    private void processAndPrintLatencies(long[] latencies) {
        if (latencies == null || latencies.length == 0) {
            System.out.println("No latency data recorded.");
            return;
        }

        long totalTimeNano = 0;
        long minLatencyNano = Long.MAX_VALUE;
        long maxLatencyNano = Long.MIN_VALUE;

        for (long latency : latencies) {
            totalTimeNano += latency;
            if (latency < minLatencyNano) {
                minLatencyNano = latency;
            }
            if (latency > maxLatencyNano) {
                maxLatencyNano = latency;
            }
        }

        // Sort latencies to calculate percentiles
        Arrays.sort(latencies);

        long p50 = latencies[latencies.length / 2]; // Median
        long p90 = latencies[(int) (latencies.length * 0.90)];
        long p99 = latencies[(int) (latencies.length * 0.99)];
        long p999 = latencies[(int) (latencies.length * 0.999)];
        double avgLatencyNano = (double) totalTimeNano / latencies.length;

        System.out.println("--- Insertion Latency Results (" + latencies.length + " Orders) ---");
        System.out.printf(Locale.US,"Total Time: %.3f ms%n", (double) totalTimeNano / NANO_PER_MILLI);
        System.out.println("-----------------------------------------------------");
        System.out.println("Latency per Order (microseconds):");
        System.out.printf(Locale.US,"          Avg: %.3f µs%n", avgLatencyNano / NANO_PER_MICRO);
        System.out.printf(Locale.US,"          Min: %.3f µs%n", (double) minLatencyNano / NANO_PER_MICRO);
        System.out.printf(Locale.US,"          Max: %.3f µs%n", (double) maxLatencyNano / NANO_PER_MICRO);
        System.out.printf(Locale.US,"        P50th: %.3f µs (Median)%n", (double) p50 / NANO_PER_MICRO);
        System.out.printf(Locale.US,"        P90th: %.3f µs%n", (double) p90 / NANO_PER_MICRO);
        System.out.printf(Locale.US,"        P99th: %.3f µs%n", (double) p99 / NANO_PER_MICRO);
        System.out.printf(Locale.US,"      P99.9th: %.3f µs%n", (double) p999 / NANO_PER_MICRO);
        System.out.println("-----------------------------------------------------");

        // Optional: Check if max latency is suspiciously high (could indicate resize or GC)
        if (maxLatencyNano > p999 * 5) { // Arbitrary threshold
            System.out.println("WARNING: Max latency significantly higher than P99.9 - potential resize or GC event occurred.");
        }
    }
}
