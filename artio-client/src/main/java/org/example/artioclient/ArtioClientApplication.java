package org.example.artioclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.IoUtil;
import org.agrona.concurrent.IdGenerator;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.co.real_logic.artio.builder.HeartbeatEncoder;
import uk.co.real_logic.artio.builder.QuoteRequestEncoder;
import uk.co.real_logic.artio.builder.TradingSessionStatusEncoder;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.messages.SessionState;
import uk.co.real_logic.artio.session.Session;

import java.io.File;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class ArtioClientApplication implements ApplicationRunner {

    private final Session sampleSession;
    private final Session artioSession;
    private final FixLibrary library;

    public static void main(String[] args) {
        SpringApplication.run(ArtioClientApplication.class, args);
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy(100);
        while (!sampleSession.isActive() || !artioSession.isActive()) {
            idleStrategy.idle(library.poll(1));
        }

        final long nodeId = 1L;
        final IdGenerator idGenerator = new SnowflakeIdGenerator(nodeId);
        final HeartbeatEncoder testRequest = new HeartbeatEncoder();
        testRequest.testReqID("SendingFromArtioClient_" + idGenerator.nextId());
        sampleSession.trySend(testRequest);

        final TradingSessionStatusEncoder tradingStatus = new TradingSessionStatusEncoder();
        tradingStatus.tradingSessionID("sessionId_" + artioSession.id());
        tradingStatus.tradSesStatus(artioSession.state().value());

        artioSession.trySend(tradingStatus);

        while (artioSession.state() != SessionState.DISCONNECTED) {
            idleStrategy.idle(library.poll(1));
        }

    }


}


