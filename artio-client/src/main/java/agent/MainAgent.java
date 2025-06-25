package agent;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class MainAgent implements Agent {
    private final CountDownLatch workerStartLatch;
    private boolean isReady = false;

    public MainAgent(CountDownLatch workerStartLatch) {
        this.workerStartLatch = workerStartLatch;
    }

    @Override
    public void onStart() {
        log.info("Waiting for all worker agents to start...");
        try {
            workerStartLatch.await();
            isReady = true;
            log.info("All workers are ready! Main agent is now starting its work.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("MainAgent was interrupted while waiting for workers.", e);
        }
    }

    @SneakyThrows
    @Override
    public int doWork() {
        if (isReady) {
            Thread.sleep(1000);
            log.info("Performing main work cycle...");
        }
        return 1;
    }

    @Override
    public void onClose() {
        log.info("Closing down.");
    }

    @Override
    public String roleName() {
        return "main-agent";
    }
}
