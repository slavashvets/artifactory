package org.artifactory.storage.binstore.binary.providers.base;

import java.util.Map;

/**
 * @author Gidi Shabat
 */
public interface BinaryProviderServices {

    void notifyError(String id, String type);

    Map<String, Class> getBinaryProvidersMap();

}
