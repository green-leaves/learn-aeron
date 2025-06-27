package org.example.artioclient;

import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import org.agrona.IoUtil;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.status.AtomicCounter;
import uk.co.real_logic.artio.CommonConfiguration;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.aeron.CommonContext.IPC_CHANNEL;
import static io.aeron.driver.ThreadingMode.SHARED;
import static org.example.artioclient.CommonConfigs.*;
import static uk.co.real_logic.artio.CommonConfiguration.optimalTmpDirName;

public final class SampleUtil {
    public static FixLibrary blockingConnect(final LibraryConfiguration configuration) {
        final FixLibrary library = FixLibrary.connect(configuration);
        while (!library.isConnected()) {
            library.poll(1);
            Thread.yield();
        }
        return library;
    }

    public static void runAgentUntilSignal(
            final Agent agent, final MediaDriver mediaDriver) throws InterruptedException {
        final AtomicCounter errorCounter =
                mediaDriver.context().countersManager().newCounter("exchange_agent_errors");
        final AgentRunner runner = new AgentRunner(
                CommonConfiguration.backoffIdleStrategy(),
                Throwable::printStackTrace,
                errorCounter,
                agent);

        final Thread thread = AgentRunner.startOnThread(runner);

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        while (running.get()) {
            Thread.sleep(100);
        }

        thread.join();
    }

    public static void startFixEngine() {
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

        EngineConfiguration engineConfiguration = new EngineConfiguration()
                .libraryAeronChannel(IPC_CHANNEL)
                .monitoringFile(optimalTmpDirName() + File.separator + "fix-client" + File.separator + "engineCounters")
                .logFileDir("aeron-client-logs");

        IoUtil.delete(new File(engineConfiguration.logFileDir()), true);
        engineConfiguration.aeronArchiveContext()
                .aeronDirectoryName(AERON_DIR_NAME)
                .controlRequestChannel(CONTROL_REQUEST_CHANNEL)
                .controlResponseChannel(CONTROL_RESPONSE_CHANNEL);

        engineConfiguration.aeronContext()
                .aeronDirectoryName(AERON_DIR_NAME);
        ArchivingMediaDriver.launch(context, archiveContext);
        FixEngine.launch(engineConfiguration);
    }
}
