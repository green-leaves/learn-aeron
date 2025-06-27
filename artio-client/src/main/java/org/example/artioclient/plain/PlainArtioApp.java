package org.example.artioclient.plain;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.artio.builder.TradingSessionStatusEncoder;
import uk.co.real_logic.artio.library.SessionConfiguration;

import static org.example.artioclient.SampleUtil.startFixEngine;

public class PlainArtioApp {

    public static void main(String[] args) {
        startFixEngine();

        final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                .address("localhost", 9882)
                .senderCompId("ARTIO-AGENT")
                .targetCompId("QUICKFIX-9982")
                .resetSeqNum(true)
                .build();

        PlainAgent plainAgent = new PlainAgent(sessionConfig, new PlainSessionHandler());

        AgentRunner plainAgentRunner = new AgentRunner(
                new SleepingIdleStrategy(100),
                Throwable::printStackTrace,
                null,
                plainAgent);

        AgentRunner.startOnThread(plainAgentRunner, Thread::new);


        final TradingSessionStatusEncoder tradingStatus = new TradingSessionStatusEncoder();
        tradingStatus.tradingSessionID("sessionId_plain");
        tradingStatus.tradSesStatus(plainAgent.getSession().state().value());
        tradingStatus.text("Hello World! From Plain Agent!");
        plainAgent.getSession().trySend(tradingStatus);
    }


}
