package oms;

import org.agrona.collections.*;

import java.util.Arrays;
import java.util.Comparator;

public class OrderBook {

    private static final int INITIAL_CAPACITY = 1024;
    private static final long PRICE_SCALING_FACTOR = 1_000_000L;
    private static final long NOT_FOUND_VALUE = -1L;
    private static final byte BID = 0; // Client wants to BUY at target
    private static final byte ASK = 1; // Client wants to SELL at target
    private static final byte STATUS_ACTIVE = 0;
    private static final byte STATUS_TRIGGERED = 1;
    private static final byte STATUS_CANCELLED = 2;

    private int capacity;
    private int size = 0;
    private long[] orderIds;
    private long[] clientIds;
    private long[] scaledTargetRates;
    private byte[] sides;
    private byte[] statuses;

    private final Long2ObjectHashMap<IntArrayList> rateToOrderIndicesMap;
    private final LongArrayList activeBidTargetRates; // Client SELL order Low  to High
    private final LongArrayList activeAskTargetRates; // Client BUY  order High to Low
    private final Comparator<Long> bidRateComparator = Comparator.naturalOrder();
    private final Comparator<Long> askRateComparator = Comparator.reverseOrder();
    private final Long2LongHashMap orderIdToIndexMap;


    public OrderBook() {
        this.capacity = INITIAL_CAPACITY;
        this.orderIds = new long[capacity];
        this.clientIds = new long[capacity];
        this.scaledTargetRates = new long[capacity];
        this.sides = new byte[capacity];
        this.statuses = new byte[capacity];

        rateToOrderIndicesMap = new Long2ObjectHashMap<>();
        activeBidTargetRates = new LongArrayList();
        activeAskTargetRates = new LongArrayList();
        orderIdToIndexMap = new Long2LongHashMap(NOT_FOUND_VALUE);
    }

    public boolean addOrder(long orderId, long clientId, double targetRate, boolean isBid) {
        if (orderIdToIndexMap.containsKey(orderId)) {
            System.err.println("Order ID already exists: " + orderId);
            return false;
        }

        if (size == capacity) {
            if (!resize()) return false;
        }

        int currentIdx = size;
        byte side = isBid ? BID : ASK;
        long scaledRate = scaleRate(targetRate);

        this.orderIds[currentIdx] = orderId;
        this.clientIds[currentIdx] = clientId;
        this.scaledTargetRates[currentIdx] = scaledRate;
        this.sides[currentIdx] = side;
        this.statuses[currentIdx] = STATUS_ACTIVE;
        orderIdToIndexMap.put(orderId, currentIdx);

        IntArrayList indicesAtRate = rateToOrderIndicesMap.get(scaledRate);
        if (indicesAtRate == null) {
            indicesAtRate = new IntArrayList();
            rateToOrderIndicesMap.put(scaledRate, indicesAtRate);
            addRateToSortedList(scaledRate, side);
        }
        indicesAtRate.add(currentIdx); // Use addInt for primitive int index

        this.size++;

        return true;
    }

    /**
     * Checks the current market rate (Bid/Ask) against all active target orders.
     * Finds all orders whose conditions are met and adds their IDs to the results.
     * Marks triggered orders as inactive.
     *
     * @param marketBidRate The current market BID price.
     * @param marketAskRate The current market ASK price.
     * @param triggeredOrderIds List to populate with IDs of triggered orders.
     */
    public void match(double marketBidRate, double marketAskRate, LongArrayList triggeredOrderIds) {
        long scaledMarketBid = scaleRate(marketBidRate);
        long scaledMarketAsk = scaleRate(marketAskRate);

        for (int i = 0; i < activeBidTargetRates.size(); i++) {
             long clientBidTargetRate = activeBidTargetRates.get(i);
             if (scaledMarketBid >= clientBidTargetRate) {
                processMatchOrder(clientBidTargetRate, triggeredOrderIds);
             } else {
                 break;
             }
        }

        for (int i = 0; i < activeAskTargetRates.size(); i++) {
            long clientAskTargetRate = activeAskTargetRates.get(i);
            if (scaledMarketAsk <= clientAskTargetRate) {
                processMatchOrder(clientAskTargetRate, triggeredOrderIds);
            } else {
                break;
            }
        }
    }

    private void processMatchOrder(long clientTargetRate, LongArrayList triggeredOrderIds) {
        IntArrayList indicesAtRate = rateToOrderIndicesMap.get(clientTargetRate);
        if (indicesAtRate != null) {
            for (int i = 0; i < indicesAtRate.size(); i++) {
                 int orderIndex = indicesAtRate.getInt(i);
                 if (statuses[orderIndex] == STATUS_ACTIVE) {
                     triggeredOrderIds.add(orderIds[orderIndex]);
                 }
            }
        }
    }

    /**
     * Adds the scaledRate to the appropriate sorted list (activeBuyTargetRates or
     * activeSellTargetRates) maintaining sorted order.
     * This method MODIFIES the member lists directly.
     *
     * @param scaledRate The rate to add.
     * @param side SIDE_BUY or SIDE_SELL to determine which list to use.
     */
    private void addRateToSortedList(long scaledRate, byte side) {
        LongArrayList list = (side == BID) ? activeBidTargetRates : activeAskTargetRates;
        Comparator<Long> comparator = (side == BID) ? bidRateComparator : askRateComparator;
        int insertionPoint = findRateInsertionPoint(list, comparator, scaledRate);
        list.addLong(insertionPoint, scaledRate);
    }

    private int findRateInsertionPoint(LongArrayList list, Comparator<Long> comparator, long rate) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = list.getLong(mid);
            int cmp = comparator.compare(midVal, rate);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return low;
    }

    private long scaleRate(double rate) {
        return (long) (rate * PRICE_SCALING_FACTOR + (rate >= 0 ? 0.5 : -0.5)); // Basic rounding
    }

    private boolean resize() {
        int oldCapacity = capacity;
        if (oldCapacity >= Integer.MAX_VALUE / 2) return false;
        capacity = Math.min(Integer.MAX_VALUE - 8, capacity * 2);

        try {
            System.out.println("Resizing target rate store arrays to " + capacity);
            orderIds = Arrays.copyOf(orderIds, capacity);
            clientIds = Arrays.copyOf(clientIds, capacity);
            scaledTargetRates = Arrays.copyOf(scaledTargetRates, capacity);
            sides = Arrays.copyOf(sides, capacity);
            statuses = Arrays.copyOf(statuses, capacity);
            return true;
        } catch (OutOfMemoryError oom) {
            System.err.println("OOM during resize to capacity " + capacity);
            capacity = oldCapacity;
            return false;
        }
    }

}
