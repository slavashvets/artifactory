package org.artifactory.storage.binstore.ifc;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.artifactory.storage.binstore.binary.providers.ExternalWrapperBinaryProviderImpl;
import org.artifactory.storage.binstore.binary.providers.base.BinaryProvider;
import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.binstore.ifc.model.BinaryTreeElement;

/**
 * @author gidis
 */
public interface BinaryProviderManager {

    BinaryTreeElement<BinaryProviderInfo> getBinaryProvidersInfo();

    boolean optimize(boolean force);

    void ping();

    ObjectInputStream getBinariesDataStream() throws IOException;

    List<String> getErrors();

    ExternalWrapperBinaryProviderImpl getExternalWrapperBinaryProvider(File externalDir);

    BinaryProvider getBinaryProvider();

    void initializeExternalBinaryProvider(String mode, String externalDir, String absolutePath,
            BinaryProviderConfig defaultValues);
}
