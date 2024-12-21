package org.example.artioclient.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.*;
import org.agrona.concurrent.status.AtomicCounter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.co.real_logic.artio.builder.HeartbeatEncoder;
import uk.co.real_logic.artio.session.Session;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class ArtioSpringApplication implements ApplicationRunner {

    private final Session sampleSession;
    private final FixMonitoringAgent fixMonitoringAgent;
    private final FixLibraryContext libraryContext;

    public static void main(String[] args) {
        SpringApplication.run(ArtioSpringApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        final long nodeId = 1L;
        final IdGenerator idGenerator = new SnowflakeIdGenerator(nodeId);
        final HeartbeatEncoder testRequest = new HeartbeatEncoder();
        testRequest.testReqID("SendingFromArtioClient_" + idGenerator.nextId());
        sampleSession.trySend(testRequest);

        AtomicCounter errorCounter = libraryContext.getMediaDriver()
                .mediaDriver().context().countersManager().newCounter("fix.client_errors");

        AgentRunner fixMonitoringAgentRunner = new AgentRunner(
                new BackoffIdleStrategy(),
                Throwable::printStackTrace,
                errorCounter,
                fixMonitoringAgent);

        Thread thread = AgentRunner.startOnThread(fixMonitoringAgentRunner);

        Thread.startVirtualThread(thread);
    }


}


