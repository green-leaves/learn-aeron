package oms;

import java.util.Arrays;
import java.util.Locale;

public class TestUtils {
    private static final String[] pairs = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "EURCAD"};
    public static final long NANO_PER_MICRO = 1000L;
    public static final long NANO_PER_MILLI = 1_000_000L;

    public static String randomCcyPair() {
        return pairs[(int) (Math.random() * pairs.length)];
    }

    public static double generateRandomRate(double base) {
        return base + 0.000100 + (Math.random() * (0.000999 - 0.000100));
    }

    public static void processAndPrintLatencies(long[] latencies) {
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

        System.out.println("--- Latency Results (" + latencies.length + " Records) ---");
        System.out.printf(Locale.US,"Total Time: %.3f ms%n", (double) totalTimeNano / NANO_PER_MILLI);
        System.out.println("-----------------------------------------------------");
        System.out.println("Latency per record (microseconds):");
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
