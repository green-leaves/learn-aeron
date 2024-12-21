package org.example.artioclient.spring;

import lombok.Getter;
import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryUtil;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

@Getter
public class SessionContext {
    private final SessionConfiguration sessionConfiguration;
    private final FixLibrary library;
    private final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy();
    private Session session;

    public SessionContext(SessionConfiguration sessionConfiguration,
                          FixLibrary library) {
        this.sessionConfiguration = sessionConfiguration;
        this.library = library;
        this.session = LibraryUtil.initiate(
                library,
                sessionConfiguration,
                10_000,
                idleStrategy);
    }

    public void reset() {
        this.session = LibraryUtil.initiate(
                library,
                sessionConfiguration,
                10_000,
                idleStrategy);
    }
}
