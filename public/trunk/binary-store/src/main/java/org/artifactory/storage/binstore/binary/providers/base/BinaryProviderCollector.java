package org.artifactory.storage.binstore.binary.providers.base;

/**
 * @author Gidi Shabat
 */
public interface BinaryProviderCollector<T> {
    void collect(BinaryProviderBase binaryProviderBase);
}
