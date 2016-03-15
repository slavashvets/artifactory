package org.artifactory.storage.binstore.binary.providers;

/**
 * @author Fred Simon
 */
public interface StateAwareBinaryProvider {

    boolean tryToActivate();

    boolean isActive();

}
