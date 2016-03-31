package org.artifactory.storage.binstore.binary.providers.base;

/**
 * @author Gidi Shabat
 */
public class MutableBinaryProviderInjector {
    public static MutableBinaryProvider getMutableBinaryProvider(BinaryProviderBase binaryProviderBase){
        return binaryProviderBase.mutableBinaryProvider;
    }

    public static void setMutableBinaryProvider(BinaryProviderBase binaryProviderBase,
            MutableBinaryProvider mutableBinaryProvider){
        binaryProviderBase.mutableBinaryProvider=mutableBinaryProvider;
    }
}
