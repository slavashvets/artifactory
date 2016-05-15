package org.artifactory.storage.binstore.service;


/**
 * Listener interface used in Binary Store to get event during Artifactory File store GC steps.
 *
 * @author Gidi Shabat
 */
public interface GarbageCollectorListener {
    /**
     * The garbage collection is starting.
     * Called at the very beginning of the process.
     */
    void start();

    /**
     * Once all deletion was done this is called with result data object.
     */
    void finished();

    /**
     * Called when Artifactory server is going down.
     * all resources should be cleaned after this call.
     */
    void destroy();
}
