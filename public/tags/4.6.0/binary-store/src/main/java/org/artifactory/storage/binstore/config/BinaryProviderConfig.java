package org.artifactory.storage.binstore.config;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * @author Gidi Shabat
 */
public class BinaryProviderConfig {
    private Map<String, String> params = Maps.newHashMap();
    private Map<String, String> properties = Maps.newHashMap();
    /**
     *
     */
    private String binaryStoreXmlPath;

    public String getBinaryStoreXmlPath() {
        return binaryStoreXmlPath;
    }

    public void setBinaryStoreXmlPath(String binaryStoreXmlPath) {
        this.binaryStoreXmlPath = binaryStoreXmlPath;
    }

    public void addParam(String key, String value) {
        params.put(key, value);
    }

    public void addProperty(String key, String value) {
        properties.put(key,value);
    }

    public String getParam(String key) {
        return params.get(key);
    }

    public Map<String, String> getProperties(String prefix) {
        Map<String, String> result = Maps.newHashMap();
        properties.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(key -> {
            String newKey = key.replaceFirst(prefix, "");
            result.put(newKey, properties.get(key));
        });
        return result;
    }


}
