package org.example.artioclient;

import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.ControlledFragmentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.IdGenerator;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.co.real_logic.artio.builder.HeartbeatEncoder;
import uk.co.real_logic.artio.builder.Printer;
import uk.co.real_logic.artio.decoder.PrinterImpl;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.*;
import uk.co.real_logic.artio.messages.DisconnectReason;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.util.AsciiBuffer;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import java.io.File;

import static io.aeron.driver.ThreadingMode.SHARED;
import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;
import static java.util.Collections.singletonList;
import static uk.co.real_logic.artio.CommonConfiguration.optimalTmpDirName;
import static uk.co.real_logic.artio.messages.SessionState.DISCONNECTED;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class ArtioClientApplication implements ApplicationRunner {

    private static final String AERON_DIR_NAME = "artio-client/target/client-aeron";
    private static final String ARCHIVE_DIR_NAME = "artio-client/target/client-aeron-archive";
    private static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:7010";
    private static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:7020";
    private static final String RECORDING_EVENTS_CHANNEL = "aeron:udp?control-mode=dynamic|control=localhost:7030";
    private static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";

    private final SampleSessionHandler sampleSessionHandler;

    public static void main(String[] args) {
        SpringApplication.run(ArtioClientApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Static configuration lasts the duration of a FIX-Gateway instance
        // final String aeronChannel = "aeron:udp?endpoint=localhost:10002";
        final String aeronChannel = "aeron:ipc";
        try (EngineConfiguration configuration = new EngineConfiguration()
                .libraryAeronChannel(aeronChannel)
                .monitoringFile(optimalTmpDirName() + File.separator + "fix-client" + File.separator + "engineCounters")
                .logFileDir("aeron-client-logs")) {

            configuration.aeronArchiveContext()
                    .aeronDirectoryName(AERON_DIR_NAME)
                    .controlRequestChannel(CONTROL_REQUEST_CHANNEL)
                    .controlResponseChannel(CONTROL_RESPONSE_CHANNEL);

            configuration.aeronContext()
                    .aeronDirectoryName(AERON_DIR_NAME);
            final MediaDriver.Context context = new MediaDriver.Context()
                    .threadingMode(SHARED)
                    .dirDeleteOnStart(true)
                    .aeronDirectoryName(AERON_DIR_NAME);

            final Archive.Context archiveContext = new Archive.Context()
                    .threadingMode(ArchiveThreadingMode.SHARED)
                    .deleteArchiveOnStart(true)
                    .aeronDirectoryName(AERON_DIR_NAME)
                    .archiveDirectoryName(ARCHIVE_DIR_NAME);

            archiveContext
                    .controlChannel(CONTROL_REQUEST_CHANNEL)
                    .replicationChannel(REPLICATION_CHANNEL)
                    .recordingEventsChannel(RECORDING_EVENTS_CHANNEL);

            try (ArchivingMediaDriver driver = ArchivingMediaDriver.launch(context, archiveContext)) {
                try (FixEngine ignore = FixEngine.launch(configuration)) {
                    // Each outbound session with an Exchange or broker is represented by
                    // a Session object. Each session object can be configured with connection
                    // details and credentials.
                    final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                            .address("localhost", 9880)
                            .targetCompId("EXEC")
                            .senderCompId("BANZAI")
                            .resetSeqNum(true)
                            .build();

                    final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy(100);

                    final LibraryConfiguration libraryConfiguration = new LibraryConfiguration()
                            .sessionAcquireHandler((session, acquiredInfo) -> sampleSessionHandler)
                            .libraryAeronChannels(singletonList(aeronChannel));

                    libraryConfiguration.aeronContext()
                            .aeronDirectoryName(AERON_DIR_NAME);

                    log.info("Starting fixLibrary");
                    try (FixLibrary library = SampleUtil.blockingConnect(libraryConfiguration)) {
                        // Whilst we only initiate a single Session for a given library here, it is
                        // perfectly possible to initiate multiple Sessions on a given library
                        // and manage them accordingly.
                        log.info("Starting session");
                        final Session session = LibraryUtil.initiate(
                                library,
                                sessionConfig,
                                10_000,
                                idleStrategy);

                        while (!session.isActive()) {
                            idleStrategy.idle(library.poll(1));
                        }
                        final long nodeId = 1L;
                        final IdGenerator idGenerator = new SnowflakeIdGenerator(nodeId);
                        final HeartbeatEncoder testRequest = new HeartbeatEncoder();

                        testRequest.testReqID("SendingFromArtioClient_" + idGenerator.nextId());

                        session.trySend(testRequest);


                        while (session.state() != DISCONNECTED) {
                            idleStrategy.idle(library.poll(1));
                        }
                    }
                }
            }

        }

        //cleanupOldLogFileDir(configuration);
    }

    private static SessionHandler onConnect(final Session session, SessionAcquiredInfo acquiredInfo) {
        final AsciiBuffer string = new MutableAsciiBuffer();
        final Printer printer = new PrinterImpl();
        return new SessionHandler() {
            @Override
            public ControlledFragmentHandler.Action onMessage(DirectBuffer buffer,
                                                              int offset,
                                                              int length,
                                                              int libraryId,
                                                              Session session,
                                                              int sequenceIndex,
                                                              long messageType,
                                                              long timestampInNs,
                                                              long position,
                                                              OnMessageInfo messageInfo) {
                string.wrap(buffer);
                log.info("onMessage {}, {}", session.id(), printer.toString(string, offset, length, messageType));
                return null;
            }

            @Override
            public void onTimeout(int i, Session session) {
                log.info("onTimeout");
            }

            @Override
            public void onSlowStatus(int i, Session session, boolean b) {

            }

            @Override
            public ControlledFragmentHandler.Action onDisconnect(int i, Session session, DisconnectReason disconnectReason) {
                log.info("onDisconnect {}", disconnectReason.name());
                return CONTINUE;
            }

            @Override
            public void onSessionStart(Session session) {
                log.info("onSessionStart {}", session.beginString());
            }
        };
    }

}


