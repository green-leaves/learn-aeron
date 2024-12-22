package org.example.artioclient.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixMonitoringAgent implements Agent {

    private final FixLibraryContext libraryContext;
    private final SessionContext artioSession;

    @Override
    public int doWork() {
        int polled = libraryContext.getFixLibrary().poll(1);
        if (!artioSession.getSession().isConnected()) {
            log.info("Artio session is closed");
            artioSession.reset();
            return polled + 1;
        }
        return polled;
    }

    @Override
    public String roleName() {
        return "";
    }
}
