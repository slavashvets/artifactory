package org.artifactory.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * A work queue that eliminates jobs based on a passed criteria.
 *
 * @author Yossi Shaul
 */
public class WorkQueue<T> {
    private static final Logger log = LoggerFactory.getLogger(WorkQueue.class);

    private final Queue<T> queue = new ConcurrentLinkedQueue<>();
    private final String name;
    private final Consumer<T> workExecutor;
    private Semaphore semaphore = new Semaphore(1);

    /**
     * Creates a new Work queue.
     *
     * @param name         Symbolic name for the work queue
     * @param workExecutor The work to perform for each element in the queue
     */
    public WorkQueue(String name, Consumer<T> workExecutor) {
        this.name = name;
        this.workExecutor = workExecutor;
    }

    /**
     * Offer a new work to the queue. If the work is accepted and there's no other worker thread, the offering thread
     * continues as the worker.
     *
     * @param w The work to perform
     * @return True if the work was accepted
     */
    public void offerWork(T w) {
        //TODO: [by YS] do it atomic
        if (queue.contains(w)) {
            log.trace("{}: contains '{}'", name, w);
            return;
        } else {
            log.trace("{}: adding '{}'", name, w);
            queue.add(w);
        }

        if (!semaphore.tryAcquire()) {
            log.trace("{}: already processing", name);
            return;
        }
        try {
            log.debug("{}: start processing. Queue size: {}", name, queue.size());
            T workItem;
            while ((workItem = queue.poll()) != null) {
                try {
                    workExecutor.accept(workItem);
                } catch (Exception e) {
                    log.error("{}: failed to process: {}", workItem, e);
                }
            }
        } finally {
            semaphore.release();
        }
    }
}
