package org.artifactory.storage.binstore.binary.providers.base;

import java.io.Serializable;

/**
 * @author gidis
 */
public interface StorageInfo extends Serializable {
    long getFreeSpace();

    long getTotalSpace();

    long getUsageSpace();

    long getUsageSpaceInPercent();

    long getFreeSpaceInPercent();
}
