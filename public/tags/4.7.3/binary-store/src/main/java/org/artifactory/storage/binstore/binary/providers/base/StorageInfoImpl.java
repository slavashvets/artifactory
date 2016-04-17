package org.artifactory.storage.binstore.binary.providers.base;

/**
 * @author gidis
 */
public class StorageInfoImpl implements StorageInfo {
    private long freeSpace;
    private long totalSpace;
    private long usageSpace;
    private long usageSpaceInPercent;
    private long freeSpaceInPercent;

    public StorageInfoImpl(long freeSpace, long totalSpace, long usageSpace, long usageSpaceInPercent,
                           long freeSpaceInPercent) {
        this.freeSpace = freeSpace;
        this.totalSpace = totalSpace;
        this.usageSpace = usageSpace;
        this.usageSpaceInPercent = usageSpaceInPercent;
        this.freeSpaceInPercent = freeSpaceInPercent;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    public long getUsageSpace() {
        return usageSpace;
    }

    public long getUsageSpaceInPercent() {
        return usageSpaceInPercent;
    }

    public long getFreeSpaceInPercent() {
        return freeSpaceInPercent;
    }
}
