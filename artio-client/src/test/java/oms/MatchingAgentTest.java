package oms;

import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static oms.TestUtils.*;

public class MatchingAgentTest {

    private static final int NUMBER_OF_ORDERS = 2000;
    private static final int BUFFER_SIZE = 16384 + RingBufferDescriptor.TRAILER_LENGTH;;

    @Test
    public void testMatchingAgent() throws Exception {
        long[] latencies = new long[NUMBER_OF_ORDERS];
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        RingBuffer ringBuffer = new ManyToOneRingBuffer(buffer);

        MatchingAgent matchingAgent = new MatchingAgent(ringBuffer);
        drainRingBuffer(matchingAgent);
        for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
            addOrder(i, ringBuffer);
        }
        int i = 0;
        int workCount;
        while (i < 10000) {
            marketRateUpdate(ringBuffer);

            long startTime = System.nanoTime();
            workCount = matchingAgent.doWork();
            long endTime = System.nanoTime();

            if (workCount <= 0) {
                break;
            }

            latencies[i++] = endTime - startTime;
        }

        processAndPrintLatencies(latencies);
    }

    @Test
    public void testMatchingAgent_SameLoop() throws Exception {
        long[] latencies = new long[NUMBER_OF_ORDERS];
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        RingBuffer ringBuffer = new ManyToOneRingBuffer(buffer);

        MatchingAgent matchingAgent = new MatchingAgent(ringBuffer);
        drainRingBuffer(matchingAgent);
        for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
            drainRingBuffer(matchingAgent);
            addOrder(i, ringBuffer);
            long startTime = System.nanoTime();
            matchingAgent.doWork();
            long endTime = System.nanoTime();
            latencies[i] = endTime - startTime;
        }

        processAndPrintLatencies(latencies);
    }

    private void addOrder(int i, RingBuffer ringBuffer) {
        // Generate unique order data
        long orderId = i + 1000L; // Start from 1000
        long clientId = 5000L + (i % 100); // Cycle through some client IDs
        // Generate slightly varying rates to populate different levels
        double targetRate = generateRandomRate(1.35000);
        byte side = (byte) ((i % 2 == 0) ? 0 : 1); // Alternate sides

        String ccyPair = randomCcyPair();
        byte[] ccyPairBytes = ccyPair.getBytes(StandardCharsets.UTF_8);
        int requiredLength = Long.BYTES     // orderId
                + Long.BYTES                // clientId
                + Double.BYTES              // targetRate
                + Byte.BYTES                // side
                + Integer.BYTES             // ccyPairLength
                + ccyPairBytes.length;      // ccyPair

        int claimIndex = ringBuffer.tryClaim(MatchingAgent.ADD_TARGET_ORDER_MSG_TYPE_ID, requiredLength);
        if (claimIndex > 0) {
            AtomicBuffer bufferToCommit = ringBuffer.buffer();
            int offset = 0;

            bufferToCommit.putLong(claimIndex + offset, orderId);
            offset += Long.BYTES;

            bufferToCommit.putLong(claimIndex + offset, clientId);
            offset += Long.BYTES;

            bufferToCommit.putDouble(claimIndex + offset, targetRate);
            offset += Double.BYTES;

            bufferToCommit.putByte(claimIndex + offset, side);
            offset += Byte.BYTES;

            // 💡 Add string length first
            bufferToCommit.putInt(claimIndex + offset, ccyPairBytes.length);
            offset += Integer.BYTES;

            // Then string bytes
            bufferToCommit.putBytes(claimIndex + offset, ccyPairBytes);

            ringBuffer.commit(claimIndex);
        }
    }

    private void marketRateUpdate(RingBuffer ringBuffer) {
        String ccyPair = randomCcyPair();
        byte[] ccyPairBytes = ccyPair.getBytes(StandardCharsets.UTF_8);
        int requiredLength = Double.BYTES       // marketRateBid
                + Double.BYTES                  // marketRateAsk
                + Integer.BYTES                 // ccyPairLength
                + ccyPairBytes.length;          // ccyPair

        int claimIndex = ringBuffer.tryClaim(MatchingAgent.MARKET_RATE_UPDATE_MSG_TYPE_ID, requiredLength);
        if (claimIndex > 0) {
            AtomicBuffer bufferToCommit = ringBuffer.buffer();
            int offset = 0;

            double marketRateBid = generateRandomRate(1.35000);
            bufferToCommit.putDouble(claimIndex + offset, marketRateBid);
            offset += Double.BYTES;

            double marketRateAsk = generateRandomRate(1.35000);
            bufferToCommit.putDouble(claimIndex + offset, marketRateAsk);
            offset += Double.BYTES;

            // 💡 Add string length first
            bufferToCommit.putInt(claimIndex + offset, ccyPairBytes.length);
            offset += Integer.BYTES;

            // Then string bytes
            bufferToCommit.putBytes(claimIndex + offset, ccyPairBytes);

            ringBuffer.commit(claimIndex);
        }

    }

    private void drainRingBuffer(Agent agent) throws Exception {
        int drainedCount = 0;
        while (agent.doWork() > 0 && drainedCount < 100) {
            // Keep calling doWork until it returns 0 (no messages processed)
            // Add a safety break to prevent infinite loops if something goes wrong
            drainedCount++;
        }
        if(drainedCount >= 100) {
            System.err.println("Warning: Drain loop executed 100 times. Buffer might not be empty.");
        }
        // A tiny sleep might sometimes help ensure OS/thread scheduling allows processing
        // Thread.sleep(0, 100); // Sleep 0ms, 100ns (very short yield) - use cautiously
    }
}
