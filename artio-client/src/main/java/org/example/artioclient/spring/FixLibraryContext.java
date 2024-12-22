package org.example.artioclient.spring;

import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.agrona.IoUtil;
import org.example.artioclient.SampleUtil;
import org.springframework.stereotype.Component;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionHandler;
import uk.co.real_logic.artio.session.Session;

import java.io.File;

import static io.aeron.CommonContext.IPC_CHANNEL;
import static io.aeron.driver.ThreadingMode.SHARED;
import static java.util.Collections.singletonList;
import static org.example.artioclient.CommonConfigs.*;

@Slf4j
@Getter
@Component
public class FixLibraryContext {

    private final FixLibrary fixLibrary;
    private final ArchivingMediaDriver mediaDriver;
    private final FixEngine fixEngine;

    public FixLibraryContext(EngineConfiguration engineConfiguration,
                             SessionHandler sampleSessionHandler) {
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

        IoUtil.delete(new File(engineConfiguration.logFileDir()), true);

        this.mediaDriver = ArchivingMediaDriver.launch(context, archiveContext);
        this.fixEngine = FixEngine.launch(engineConfiguration);

        final LibraryConfiguration libraryConfiguration = new LibraryConfiguration()
                .sessionAcquireHandler((session, acquiredInfo) -> sampleSessionHandler)
                .libraryAeronChannels(singletonList(IPC_CHANNEL));

        libraryConfiguration.aeronContext()
                .aeronDirectoryName(AERON_DIR_NAME);

        this.fixLibrary = SampleUtil.blockingConnect(libraryConfiguration);
    }

    @PreDestroy
    public void preDestroy() {
        log.info("Shutting down FixLibraryCtx");
        this.fixLibrary.sessions().forEach(Session::logoutAndDisconnect);
        this.fixLibrary.close();
        this.fixEngine.close();
        this.mediaDriver.close();
        log.info("FixLibraryCtx shut down fixEngineClosed={}, fixLibraryClose={}",
                fixEngine.isClosed(), fixLibrary.isClosed());
    }

}
