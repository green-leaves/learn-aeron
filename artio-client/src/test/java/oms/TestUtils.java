package oms;

public class TestUtils {
    private static final String[] pairs = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "EURCAD"};

    public static String randomCcyPair() {
        return pairs[(int) (Math.random() * pairs.length)];
    }

    public static double generateRandomRate(double base) {
        return base + 0.000100 + (Math.random() * (0.000999 - 0.000100));
    }
}
