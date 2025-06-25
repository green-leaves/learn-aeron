package agent;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class SequenceAgentRunner {
    public static void main(String[] args) {

        final int numWorkerAgents = 3;
        final var startLatch = new CountDownLatch(numWorkerAgents);

        final List<Agent> agents = new ArrayList<>();

        // ***************************************************************
        // *** THE FIX: Add Worker Agents FIRST, Main Agent LAST         ***
        // ***************************************************************

        // 1. Create and add the worker agents
        for (int i = 0; i < numWorkerAgents; i++) {
            // Let's add a log to the worker's doWork to see the order
            agents.add(new WorkerAgent(i, startLatch));
        }

        // 2. Add the MainAgent last
        final Agent mainAgent = new MainAgent(startLatch);
        agents.add(mainAgent);

        // The rest of the code is unchanged...
        final Agent compositeAgent = new CompositeAgent(agents);

        final AgentRunner runner = new AgentRunner(
                new SleepingIdleStrategy(100),
                (throwable) -> log.error("AgentRunner uncaught exception", throwable),
                null,
                compositeAgent
        );

        final Thread agentThread = new Thread(runner);
        agentThread.setName("agent-runner-thread");
        agentThread.start();
        log.info("AgentRunner started on thread [{}]. Press Ctrl+C to exit.", agentThread.getName());

        SigInt.register(() -> {
            log.info("Shutdown signal received, closing agent runner...");
            runner.close();
        });
    }
}
