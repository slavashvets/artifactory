package org.artifactory.maven;

import ch.qos.logback.classic.Level;
import org.artifactory.test.TestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit tests for the {@link WorkQueue}.
 *
 * @author Yossi Shaul
 */
@Test
public class WorkQueueTest {

    @BeforeClass
    public void setup() {
        TestUtils.setLoggingLevel(WorkQueue.class, Level.TRACE);
    }

    public void offerTheSameWorkConcurrently() throws InterruptedException {
        List<AtomicInteger> counts = Collections.nCopies(10, new AtomicInteger(0));
        WorkQueue<Integer> wq = new WorkQueue<>("Test Queue", workItem -> counts.get(workItem).incrementAndGet());

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                final int finalI = i;
                executorService.submit(() -> wq.offerWork(finalI));
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(1500, TimeUnit.SECONDS);
        // We expect each job to be executed at least once
        counts.forEach(count -> assertThat(count.get()).isGreaterThanOrEqualTo(1));
    }
}
