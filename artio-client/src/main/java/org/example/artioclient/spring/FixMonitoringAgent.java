package org.example.artioclient.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixMonitoringAgent implements Agent {

    private final FixLibraryContext libraryContext;
    private final SleepingIdleStrategy sleepingIdleStrategy = new SleepingIdleStrategy(100L);
    private final SessionContext artioSession;

    @Override
    public int doWork() {
        sleepingIdleStrategy.idle(libraryContext.getFixLibrary().poll(1));
        if (!artioSession.getSession().isConnected()) {
            log.info("Artio session is closed");
            artioSession.reset();
            return 1;
        }
        return 0;
    }

    @Override
    public String roleName() {
        return "";
    }
}
