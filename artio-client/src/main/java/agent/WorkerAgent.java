package agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
@Builder
@Data
public class WorkerAgent implements Agent {

    private final int id;
    private final CountDownLatch startLatch;

    @Override
    public void onStart() {
        try {
            TimeUnit.MILLISECONDS.sleep(50L * id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("{}: READY. Signaling main agent.", roleName());
        startLatch.countDown();

    }

    @Override
    public int doWork() throws Exception {
        log.info("{}: Performing worker duty cycle...", roleName());
        return 1; // Indicate work was done
    }

    @Override
    public String roleName() {
        return String.valueOf(getId());
    }
}
