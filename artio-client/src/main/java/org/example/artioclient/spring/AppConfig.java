package org.example.artioclient.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryUtil;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

import java.io.File;

import static io.aeron.CommonContext.IPC_CHANNEL;
import static org.example.artioclient.CommonConfigs.*;
import static uk.co.real_logic.artio.CommonConfiguration.optimalTmpDirName;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class AppConfig {

    private static final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy(100);

    @Bean
    public EngineConfiguration engineConfiguration() {
        try (EngineConfiguration configuration = new EngineConfiguration()
                .libraryAeronChannel(IPC_CHANNEL)
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
    public Session sampleSession(FixLibraryContext libraryContext) {
        log.info("Starting session as a bean");
        FixLibrary library = libraryContext.getFixLibrary();
        final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                .address("localhost", 9880)
                .targetCompId("EXEC")
                .senderCompId("BANZAI")
                .resetSeqNum(true)
                .build();

        return LibraryUtil.initiate(
                library,
                sessionConfig,
                10_000,
                idleStrategy);
    }

    @Bean
    public SessionContext artioSession(FixLibraryContext libraryContext) {
        FixLibrary library = libraryContext.getFixLibrary();
        log.info("Starting artio session as a bean");
        final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                .address("localhost", 9881)
                .targetCompId("QUICKFIX")
                .senderCompId("ARTIO")
                .resetSeqNum(true)
                .build();

        return new SessionContext(sessionConfig, library);
    }
}
