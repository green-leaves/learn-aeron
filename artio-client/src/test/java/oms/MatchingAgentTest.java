package oms;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static oms.TestUtils.generateRandomRate;
import static oms.TestUtils.randomCcyPair;

public class MatchingAgentTest {

    private static final int NUMBER_OF_ORDERS = 2000;
    private static final int BUFFER_SIZE = 16384 + RingBufferDescriptor.TRAILER_LENGTH;;

    @Test
    public void testMatchingAgent() {
        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        RingBuffer ringBuffer = new ManyToOneRingBuffer(buffer);

        MatchingAgent matchingAgent = new MatchingAgent(ringBuffer);

        AgentRunner matchingAgentRunner = new AgentRunner(
                new BackoffIdleStrategy(),
                Throwable::printStackTrace,
                null,
                matchingAgent);

        AgentRunner.startOnThread(matchingAgentRunner);


        for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
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

        barrier.await();
    }
}
