package org.example.artioclient;

import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.IdGenerator;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.real_logic.artio.builder.HeartbeatEncoder;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.*;
import uk.co.real_logic.artio.session.Session;

import java.io.File;

import static io.aeron.driver.ThreadingMode.SHARED;
import static java.util.Collections.singletonList;
import static uk.co.real_logic.artio.CommonConfiguration.optimalTmpDirName;
import static uk.co.real_logic.artio.messages.SessionState.DISCONNECTED;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class AppConfig {

    private static final String AERON_DIR_NAME = "artio-client/target/client-aeron";
    private static final String ARCHIVE_DIR_NAME = "artio-client/target/client-aeron-archive";
    private static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:7010";
    private static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:7020";
    private static final String RECORDING_EVENTS_CHANNEL = "aeron:udp?control-mode=dynamic|control=localhost:7030";
    private static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";

    //public static final String aeronChannel = "aeron:udp?endpoint=localhost:10002";
    private static final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy(100);
    public static final String aeronChannel = "aeron:ipc";

    private final SessionHandler sampleSessionHandler;

    @Bean
    public EngineConfiguration engineConfiguration() {

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
            return configuration;
        }
    }


    @Bean
    public FixLibrary fixLibrary(EngineConfiguration engineConfiguration,
                                 LibraryConnectHandler libraryConnectHandler) {
        // Static configuration lasts the duration of a FIX-Gateway instance
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

        ArchivingMediaDriver.launch(context, archiveContext);
        FixEngine.launch(engineConfiguration);

        final LibraryConfiguration libraryConfiguration = new LibraryConfiguration()
                .libraryConnectHandler(libraryConnectHandler)
                .sessionAcquireHandler((session, acquiredInfo) -> sampleSessionHandler)
                .libraryAeronChannels(singletonList(aeronChannel));

        libraryConfiguration.aeronContext()
                .aeronDirectoryName(AERON_DIR_NAME);

        return SampleUtil.blockingConnect(libraryConfiguration);
    }

    @Bean
    public Session sampleSession (FixLibrary library) {
        log.info("Starting session as a bean");
        final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                .address("localhost", 9880)
                .targetCompId("EXEC")
                .senderCompId("BANZAI")
                .resetSeqNum(true)
                .build();
        final Session session = LibraryUtil.initiate(
                library,
                sessionConfig,
                10_000,
                idleStrategy);

        while (!session.isActive()) {
            idleStrategy.idle(library.poll(1));
        }

        return session;
    }

    @Bean
    public Session artioSession (FixLibrary library) {
        log.info("Starting artio session as a bean");
        final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                .address("localhost", 9881)
                .targetCompId("QUICKFIX")
                .senderCompId("ARTIO")
                .resetSeqNum(true)
                .build();

        final Session session = LibraryUtil.initiate(
                library,
                sessionConfig,
                10_000,
                idleStrategy);

        while (!session.isActive()) {
            idleStrategy.idle(library.poll(1));
        }

        return session;
    }

    @Bean
    public LibraryConnectHandler libraryConnectHandler() {
        return new LibraryConnectHandler() {
            @Override
            public void onConnect(FixLibrary library) {
                log.info("Library connected");
            }

            @Override
            public void onDisconnect(FixLibrary library) {
                while (!library.isConnected())
                {
                    idleStrategy.idle(library.poll(1));
                }
            }
        };
    }
}
