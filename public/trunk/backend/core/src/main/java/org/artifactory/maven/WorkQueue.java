package org.artifactory.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * A work queue that eliminates jobs based on a passed criteria.<p>
 * The thread offering work for this queue might start working on the queue, and may work on all the queued items.
 *
 * @author Yossi Shaul
 */
public class WorkQueue<T> {
    private static final Logger log = LoggerFactory.getLogger(WorkQueue.class);

    private final Queue<T> queue = new ConcurrentLinkedQueue<>();
    private final String name;
    private final int workers;
    private final Consumer<T> workExecutor;
    private final Semaphore semaphore;

    /**
     * Creates a new work queue that allow single worker.
     *
     * @param name         Symbolic name for the work queue
     * @param workExecutor The work to perform for each element in the queue
     */
    public WorkQueue(String name, Consumer<T> workExecutor) {
        this(name, 1, workExecutor);
    }

    /**
     * Creates a new work queue with the given max workers.<p>
     * If the max workers is greater than one, the provider work executor must be thread safe.
     *
     * @param name         Symbolic name for the work queue
     * @param workers      Maximum workers allowed to work on this queue
     * @param workExecutor The work to perform for each element in the queue
     */
    public WorkQueue(String name, int workers, Consumer<T> workExecutor) {
        this.name = name;
        this.workers = workers;
        this.workExecutor = workExecutor;
        this.semaphore = new Semaphore(workers);
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
            log.debug("{}: max workers already processing ({})", name, workers);
            return;
        }
        try {
            if (log.isDebugEnabled()) {
                // queue size is expensive
                log.debug("{}: start processing. Queue size: {}", name, queue.size());
            }
            T workItem;
            while ((workItem = queue.poll()) != null) {
                if (log.isTraceEnabled()) {
                    log.trace("{}: started working on {}. Queue size: {}", name, workItem, queue.size());
                }
                try {
                    workExecutor.accept(workItem);
                    if (log.isTraceEnabled()) {
                        log.trace("{}: finished working on {}. Queue size: {}", name, workItem, queue.size());
                    }
                } catch (Exception e) {
                    log.error("{}: failed to process {}", name, workItem, e);
                }
            }
        } finally {
            semaphore.release();
        }
    }
}
