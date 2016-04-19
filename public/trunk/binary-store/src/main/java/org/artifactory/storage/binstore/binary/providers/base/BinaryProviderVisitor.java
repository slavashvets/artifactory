package org.artifactory.storage.binstore.binary.providers.base;

/**
 * @author Gidi Shabat
 */
public interface BinaryProviderVisitor<T> {
    T visit(BinaryProviderBase binaryProviderBase);
}

