package oms;

import org.agrona.DirectBuffer;
import org.agrona.collections.LongArrayList;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MatchingAgent implements Agent {

    private static final int MAX_MESSAGES_PER_READ = 10;
    public static final int MARKET_RATE_UPDATE_MSG_TYPE_ID = 1;
    public static final int ADD_TARGET_ORDER_MSG_TYPE_ID = 2;

    private final RingBuffer ringBuffer;
    private final Map<String, OrderBook> ccyPairToOrderBook;
    private final LongArrayList triggerOrderIds;

    public MatchingAgent(RingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
        this.ccyPairToOrderBook = new ConcurrentHashMap<>();
        this.triggerOrderIds = new LongArrayList();
    }

    @Override
    public int doWork() {
        return ringBuffer.read(this::handler, MAX_MESSAGES_PER_READ);
    }

    private void handler(final int msgTypeId, final DirectBuffer buffer, final int offset, final int length) {
        switch (msgTypeId) {
            case MARKET_RATE_UPDATE_MSG_TYPE_ID:
                handleMarketRateUpdate(buffer, offset);
                break;
            case ADD_TARGET_ORDER_MSG_TYPE_ID:
                handleAddTargetOrder(buffer, offset);
                break;
            default:
                //System.err.printf("[Agent %d] WARN: Received unknown message type ID: %d%n", agentId, msgTypeId);
        }
    }

    private void handleMarketRateUpdate(DirectBuffer buffer, int offset) {
        int currentOffset = offset;

        double marketBidRate = buffer.getDouble(currentOffset);
        currentOffset += Double.BYTES;

        double marketAskRate = buffer.getDouble(currentOffset);
        currentOffset += Double.BYTES;

        int ccyPairLength = buffer.getInt(currentOffset);
        currentOffset += Integer.BYTES;
        byte[] ccyPairBytes = new byte[ccyPairLength];
        buffer.getBytes(currentOffset, ccyPairBytes);

        triggerOrderIds.clear();

        String ccyPair = new String(ccyPairBytes, StandardCharsets.UTF_8);
        ccyPairToOrderBook.get(ccyPair).match(marketBidRate, marketAskRate, triggerOrderIds);
        //System.out.printf("Matched ccyPair: %s, bid: %s, ask: %s, total: %s\n", ccyPair, marketBidRate, marketAskRate, triggerOrderIds.size());
    }

    private void handleAddTargetOrder(DirectBuffer buffer, int offset) {
        int currentOffset = offset;
        long orderId = buffer.getLong(currentOffset);
        currentOffset += Long.BYTES;

        long clientId = buffer.getLong(currentOffset);
        currentOffset += Long.BYTES;

        double targetRate = buffer.getDouble(currentOffset);
        currentOffset += Double.BYTES;

        byte side = buffer.getByte(currentOffset);
        currentOffset += Byte.BYTES;

        int ccyPairLength = buffer.getInt(currentOffset);
        currentOffset += Integer.BYTES;
        byte[] ccyPairBytes = new byte[ccyPairLength];
        buffer.getBytes(currentOffset, ccyPairBytes);

        String ccyPair = new String(ccyPairBytes, StandardCharsets.UTF_8);

        ccyPairToOrderBook
                .computeIfAbsent(ccyPair, key -> new OrderBook())
                .addOrder(orderId, clientId, targetRate, side);
    }

    @Override
    public String roleName() {
        return "oms-matching-agent";
    }
}
