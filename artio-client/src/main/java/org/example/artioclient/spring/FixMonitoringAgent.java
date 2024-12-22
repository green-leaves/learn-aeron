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
    private final SessionContext artioSessionCtx;

    @Override
    public int doWork() {
        int polled = libraryContext.getFixLibrary().poll(1);
        if (!artioSessionCtx.getSession().isConnected()) {
            log.info("Trying to reconnect session");
            artioSessionCtx.reset();
            return polled + 1;
        }
        return polled;
    }

    @Override
    public String roleName() {
        return "FixMonitoringAgent";
    }
}
