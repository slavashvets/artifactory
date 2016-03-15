package org.artifactory.storage.binstore.ifc;

import com.google.common.collect.Maps;
import org.artifactory.storage.binstore.binary.providers.base.StorageInfo;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Gidi Shabat
 */
public class BinaryProviderInfo implements Serializable{
    private Map<String, String> properties = Maps.newHashMap();
    private StorageInfo storageInfo;

    public StorageInfo getStorageInfo() {
        return storageInfo;
    }

    public void setStorageInfo(StorageInfo storageInfo) {
        this.storageInfo = storageInfo;
    }

    public void addProperty(String key, String value) {
        properties.put(key,value);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
