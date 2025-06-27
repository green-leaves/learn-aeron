package org.example.artioclient.plain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.example.artioclient.SampleUtil;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.LibraryUtil;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

import static io.aeron.CommonContext.IPC_CHANNEL;
import static java.util.Collections.singletonList;
import static org.example.artioclient.CommonConfigs.AERON_DIR_NAME;

@Slf4j
@NoArgsConstructor
public class PlainAgent implements Agent {

    private FixLibrary fixLibrary;
    private SessionConfiguration sessionConfiguration;
    private PlainSessionHandler plainSessionHandler;
    private LibraryConfiguration libraryConfiguration;
    private final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy();

    @Getter
    private Session session;

    public PlainAgent(SessionConfiguration sessionConfiguration,
                      PlainSessionHandler plainSessionHandler) {
        this.sessionConfiguration = sessionConfiguration;
        this.plainSessionHandler = plainSessionHandler;
    }

    @Override
    public void onStart() {
        log.info("On start");
        libraryConfiguration = new LibraryConfiguration()
                .sessionAcquireHandler(plainSessionHandler)
                .libraryConnectHandler(plainSessionHandler)
                .libraryAeronChannels(singletonList(IPC_CHANNEL));

        libraryConfiguration.aeronContext()
                .aeronDirectoryName(AERON_DIR_NAME);

        this.fixLibrary = SampleUtil.blockingConnect(libraryConfiguration);
        log.info("Fix library started {}", fixLibrary.isConnected());
    }

    @Override
    public int doWork() throws Exception {
        checkSession();
        return fixLibrary.poll(10);
    }

    @Override
    public String roleName() {
        return "";
    }

    private void checkSession() {
        if (!fixLibrary.isConnected()) {
            log.warn("Fix library is not connected");
            this.fixLibrary.close();
            this.fixLibrary = SampleUtil.blockingConnect(libraryConfiguration);
        }

        if (this.session == null || !this.session.isConnected()) {
            this.session = LibraryUtil.initiate(
                    this.fixLibrary,
                    this.sessionConfiguration,
                    10_000,
                    idleStrategy);
        }
    }
}
