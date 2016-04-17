package org.artifactory.storage.binstore.service;

/**
 * @author gidis
 */
public interface UsageTracking {
    /**
     * Increments the active users of a certain binary to prevent deletion while still in usage.
     *
     * @param sha1 The sha1 checksum to protect
     */
    int incrementNoDeleteLock(String sha1);

    /**
     * Decrements the active users of a certain binary. Indicates that the active usage was ended.
     *
     * @param sha1 The sha1 checksum to remove protection from
     */
    void decrementNoDeleteLock(String sha1);

}
