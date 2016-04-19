package org.artifactory.storage.binstore.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.artifactory.storage.binstore.config.model.ChainMetaData;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.artifactory.storage.binstore.config.BinaryProviderConfigBuilder.buildByUserConfig;
import static org.artifactory.storage.binstore.config.BinaryProviderConfigBuilder.buildUserTemplate;


/**
 * @author Gidi Shabat
 */
public class ConfigurableBinaryProviderManager {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableBinaryProviderManager.class);

    /**
     * New generation binary providers config.
     * The method uses the new binarystore.xml in the etc dir to load and override the default-storage-config.xml file
     */
    public static ChainMetaData buildByConfig(File userConfigFile, BinaryProviderConfig defaultValues) throws IOException {
        log.debug("Using the new generation binary provider config");
        FileInputStream userConfigStream = new FileInputStream(userConfigFile);
        String defaultConfigPath = "/default-storage-config.xml";
        InputStream defaultConfigStream = ConfigurableBinaryProviderManager.class.getResource(defaultConfigPath).openStream();
        return buildByUserConfig(defaultConfigStream, userConfigStream, defaultValues);
    }

    /**
     * This method should be removed after converting the old storage.properties config to the new generation config
     * It is kind of hack to support the old storage.properties.
     */
    public static ChainMetaData buildByStorageProperties(BinaryProviderConfig defaultValues) throws IOException {
        log.debug("Using the old generation binary provider config");
        String defaultConfigPath = "/default-storage-config.xml";
        InputStream defaultConfigStream = ConfigurableBinaryProviderManager.class.getResource(defaultConfigPath).openStream();
        String binariesStorageTypeName = defaultValues.getParam("storageType");
        long cacheMaxSize = Long.parseLong(defaultValues.getParam("maxCacheSize"));
        switch (binariesStorageTypeName) {
            case "filesystem":
                if (StringUtils.isBlank(defaultValues.getParam("secondFileStoreDir"))) {
                    log.debug("Initializing 'file-system' chain");
                    ChainMetaData chain = buildUserTemplate(defaultConfigStream, "file-system");
                    fillMetaData(defaultValues, chain.getProviderMetaData());
                    return chain;
                } else {
                    log.debug("Initializing 'double' chain");
                    ChainMetaData chain = buildUserTemplate(defaultConfigStream, "double");
                    fillMetaData(defaultValues, chain.getProviderMetaData());
                    return chain;
                }
            case "cachedFS":
                if (cacheMaxSize > 0) {
                    log.debug("Initializing 'cache-fs' chain");
                    ChainMetaData chain = buildUserTemplate(defaultConfigStream, "cache-fs");
                    fillMetaData(defaultValues, chain.getProviderMetaData());
                    return chain;
                } else {
                    throw new IllegalStateException("Binary provider typed cachedFS cannot have a zero cached size!");
                }
            case "fullDb":
                if (cacheMaxSize > 0) {
                    log.debug("Initializing 'full-db' chain");
                    ChainMetaData chain = buildUserTemplate(defaultConfigStream, "full-db");
                    fillMetaData(defaultValues, chain.getProviderMetaData());
                    return chain;
                } else {
                    log.debug("Initializing 'full-db-direct' chain");
                    ChainMetaData chain = buildUserTemplate(defaultConfigStream, "full-db-direct");
                    fillMetaData(defaultValues, chain.getProviderMetaData());
                    return chain;

                }
            case "S3": {
                log.debug("Initializing 's3' chain");
                ChainMetaData chain = buildUserTemplate(defaultConfigStream, "s3");
                // Set the store dir in the file (cache) binary provider
                fillMetaData(defaultValues, chain.getProviderMetaData());
                return chain;
            }
            case "S3Old": {
                log.debug("Initializing 's3t' chain");
                ChainMetaData chain = buildUserTemplate(defaultConfigStream, "s3Old");
                fillMetaData(defaultValues, chain.getProviderMetaData());
                return chain;
            }
            case "goog": {
                log.debug("Initializing 's3t' chain");
                return buildUserTemplate(defaultConfigStream, "google-storage");
            }
            default:
                throw new RuntimeException("Fail to initiate binary provider config. Reason: invalid storage" +
                        " type in storage.properties");
        }
    }

    private static void fillMetaData(BinaryProviderConfig defaultValues, ProviderMetaData metaData) {
        if (metaData == null) {
            return;
        }
        BinaryProviderMetaDataFiller.fillMetaData(metaData, defaultValues);
        fillMetaData(defaultValues, metaData.getProviderMetaData());
        List<ProviderMetaData> subProviderMetaDataList = metaData.getSubProviderMetaDataList();
        if (subProviderMetaDataList != null) {
            for (ProviderMetaData subProviderMetaData : subProviderMetaDataList) {
                fillMetaData(defaultValues, subProviderMetaData);
            }
        }
        fillMetaData(defaultValues, metaData.getProviderMetaData());
    }

}
