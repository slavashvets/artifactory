package org.artifactory.storage.binstore.config;

import org.artifactory.storage.binstore.binary.providers.ProviderConnectMode;
import org.artifactory.storage.binstore.config.model.Param;
import org.artifactory.storage.binstore.config.model.Property;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;

import java.io.File;
import java.util.Map;

/**
 * @author Gidi Shabat
 *
 * This class is responsable to map the parameters from the default value to the metata data.
 */
public class BinaryProviderMetaDataFiller {
    public static void fillMetaData(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        switch (providerMetaData.getType()) {
            case "blob": {
                fillBlobBinaryProviderInfo();
                break;
            }
            case "sharding": {
                fillShardBinaryProviderInfo(providerMetaData);
                break;
            }
            case "s3": {
                fillS3BinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "google-storage": {
                fillGoogleStorageBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "empty": {
                fillEmptyBinaryProviderInfo();
                break;
            }
            case "eventual": {
                fillEventualBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "external-file": {
                fillExternalFileBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "external-wrapper": {
                fillExternalWrapperBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "file-system": {
                fillFileSystemBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "cache-fs": {
                fillCacheFsBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "state-aware": {
                fillStateAwareFileBinaryProviderImplInfo(providerMetaData, defaultValues);
                break;
            }
            case "hdfs": {
                fillHdfsBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "retry": {
                fillRetryBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
            case "tracking": {
                fillTrackingBinaryProviderInfo();
                break;
            }
            case "s3Old": {
                fillS3OldBinaryProviderInfo(providerMetaData, defaultValues);
                break;
            }
        }
    }

    private static void fillTrackingBinaryProviderInfo() {
        // Do nothing no default values for this provider
    }

    private static void fillRetryBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "maxTrys", defaultValues);
        addParam(providerMetaData, "interval", defaultValues);
    }

    private static void fillHdfsBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "connectionTimeout", "" + 15000);
        addParam(providerMetaData, "connectionSoTimeout", "" + 15000);
        addParam(providerMetaData, "connectionRetry", "" + 1);
        addParam(providerMetaData, "fileStoreDir", defaultValues);
        addParam(providerMetaData, "baseDataDir", defaultValues);

        String fileStoreDir = providerMetaData.getParam("fileStoreDir").getValue();
        String baseDataDir = providerMetaData.getParam("baseDataDir").getValue();
        File dataDir = new File(fileStoreDir);
        if (!dataDir.isAbsolute()) {
            dataDir = new File(new File(baseDataDir), fileStoreDir);
        }
        addParam(providerMetaData, "binariesDir", dataDir.getAbsolutePath());
        addParam(providerMetaData, "tempDir", defaultValues);
    }

    private static void fillStateAwareFileBinaryProviderImplInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "checkPeriod", "15000");
        addParam(providerMetaData, "fileStoreDir", defaultValues);
        addParam(providerMetaData, "baseDataDir", defaultValues);
        String fileStoreDir = providerMetaData.getParam("fileStoreDir").getValue();
        String baseDataDir = providerMetaData.getParam("baseDataDir").getValue();
        File dataDir = new File(fileStoreDir);
        if (!dataDir.isAbsolute()) {
            dataDir = new File(new File(baseDataDir), fileStoreDir);
        }
        addParam(providerMetaData, "binariesDir", dataDir.getAbsolutePath());
        addParam(providerMetaData, "tempDir", defaultValues);
    }

    private static void fillCacheFsBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "maxCacheSize", defaultValues);
        addParam(providerMetaData, "cacheProviderDir", defaultValues);
        addParam(providerMetaData, "baseDataDir", defaultValues);
        addParam(providerMetaData, "cacheSynchQuietPeriod", defaultValues);
        String fileStoreDir = providerMetaData.getParam("cacheProviderDir").getValue();
        String baseDataDir = providerMetaData.getParam("baseDataDir").getValue();
        File dataDir = new File(fileStoreDir);
        if (!dataDir.isAbsolute()) {
            dataDir = new File(new File(baseDataDir), fileStoreDir);
        }
        addParam(providerMetaData, "binariesDir", dataDir.getAbsolutePath());
        addParam(providerMetaData, "tempDir", defaultValues);
    }

    private static void fillFileSystemBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "fileStoreDir", defaultValues);
        addParam(providerMetaData, "baseDataDir", defaultValues);
        String fileStoreDir = providerMetaData.getParam("fileStoreDir").getValue();
        String baseDataDir = providerMetaData.getParam("baseDataDir").getValue();
        File dataDir = new File(fileStoreDir);
        if (!dataDir.isAbsolute()) {
            dataDir = new File(new File(baseDataDir), fileStoreDir);
        }
        addParam(providerMetaData, "binariesDir", dataDir.getAbsolutePath());
        addParam(providerMetaData, "tempDir", defaultValues);
    }

    private static void fillExternalWrapperBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "fileStoreDir", defaultValues);
        addParam(providerMetaData, "baseDataDir", defaultValues);
        addParam(providerMetaData, "connectMode", ProviderConnectMode.COPY_FIRST.name());
        String fileStoreDir = providerMetaData.getParam("fileStoreDir").getValue();
        String baseDataDir = providerMetaData.getParam("baseDataDir").getValue();
        File dataDir = new File(fileStoreDir);
        if (!dataDir.isAbsolute()) {
            dataDir = new File(new File(baseDataDir), fileStoreDir);
        }
        addParam(providerMetaData, "binariesDir", dataDir.getAbsolutePath());
        addParam(providerMetaData, "tempDir", defaultValues);
    }

    private static void fillExternalFileBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "externalDir", defaultValues);
        addParam(providerMetaData, "baseDataDir", defaultValues);
        String fileStoreDir = providerMetaData.getParam("externalDir").getValue();
        String baseDataDir = providerMetaData.getParam("baseDataDir").getValue();
        File dataDir = new File(fileStoreDir);
        if (!dataDir.isAbsolute()) {
            dataDir = new File(new File(baseDataDir), fileStoreDir);
        }
        addParam(providerMetaData, "binariesDir", dataDir.getAbsolutePath());
        addParam(providerMetaData, "tempDir", defaultValues);
    }

    private static void fillEventualBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "baseDataDir", defaultValues);
        addParam(providerMetaData, "timeout", defaultValues);
        addParam(providerMetaData, "hazelcastWaitingTime", defaultValues);
        addParam(providerMetaData, "dispatcherInterval", defaultValues);
        addParam(providerMetaData, "numberOfThreads", defaultValues);
        addParam(providerMetaData, "queueSize", "" + 64);
    }

    private static void fillEmptyBinaryProviderInfo() {
        // Do nothing
    }

    private static void fillGoogleStorageBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "testConnection", defaultValues.getParam("gsTestConnection"));
        addParam(providerMetaData, "useSignature", defaultValues.getParam("gsUseSignature"));
        addParam(providerMetaData, "multiPartLimit", defaultValues.getParam("gsMultiPartLimit"));
        addParam(providerMetaData, "region", defaultValues.getParam("gsRegion"));
        addParam(providerMetaData, "identity", defaultValues.getParam("gsIdentity"));
        addParam(providerMetaData, "credential", defaultValues.getParam("gsCredential"));
        addParam(providerMetaData, "bucketName", defaultValues.getParam("gsBucketName"));
        addParam(providerMetaData, "path", defaultValues.getParam("gsPath"));
        addParam(providerMetaData, "proxyIdentity", defaultValues.getParam("gsProxyIdentity"));
        addParam(providerMetaData, "proxyCredential", defaultValues.getParam("gsProxyCredential"));
        addParam(providerMetaData, "proxyPort", defaultValues.getParam("gsProxyPort"));
        addParam(providerMetaData, "proxyHost", defaultValues.getParam("gsProxyHost"));
        addParam(providerMetaData, "port", defaultValues.getParam("gsPort"));
        addParam(providerMetaData, "endpoint", defaultValues.getParam("gsEndpoint"));
        addParam(providerMetaData, "httpsOnly", defaultValues.getParam("gsHttpsOnly"));
        addParam(providerMetaData, "providerId", defaultValues.getParam("gsProviderId"));
        addParam(providerMetaData, "httpsPort", defaultValues.getParam("gsHttpsPort"));
        Map<String, String> properties = defaultValues.getProperties();
        for (String key : properties.keySet()) {
            addProperty(providerMetaData, properties, key);
        }
    }

    private static void fillS3BinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "testConnection", defaultValues.getParam("s3TestConnection"));
        addParam(providerMetaData, "useSignature", defaultValues.getParam("s3UseSignature"));
        addParam(providerMetaData, "multiPartLimit", defaultValues.getParam("s3MultiPartLimit"));
        addParam(providerMetaData, "s3AwsVersion", defaultValues.getParam("s3AwsVersion"));
        addParam(providerMetaData, "region", defaultValues.getParam("s3Region"));
        addParam(providerMetaData, "identity", defaultValues.getParam("s3Identity"));
        addParam(providerMetaData, "credential", defaultValues.getParam("s3Credential"));
        addParam(providerMetaData, "bucketName", defaultValues.getParam("s3BucketName"));
        addParam(providerMetaData, "path", defaultValues.getParam("s3Path"));
        addParam(providerMetaData, "proxyIdentity", defaultValues.getParam("s3ProxyIdentity"));
        addParam(providerMetaData, "proxyCredential", defaultValues.getParam("s3ProxyCredential"));
        addParam(providerMetaData, "proxyPort", defaultValues.getParam("s3ProxyPort"));
        addParam(providerMetaData, "proxyHost", defaultValues.getParam("s3ProxyHost"));
        addParam(providerMetaData, "port", defaultValues.getParam("s3Port"));
        addParam(providerMetaData, "endpoint", defaultValues.getParam("s3Endpoint"));
        addParam(providerMetaData, "httpsOnly", defaultValues.getParam("s3HttpsOnly"));
        addParam(providerMetaData, "providerId", defaultValues.getParam("s3ProviderId"));
        addParam(providerMetaData, "httpsPort", defaultValues.getParam("s3HttpsPort"));
        Map<String, String> properties = defaultValues.getProperties();
        for (String key : properties.keySet()) {
            addProperty(providerMetaData, properties, key);
        }
    }

    private static void addProperty(ProviderMetaData providerMetaData, Map<String, String> properties, String key) {
        if (providerMetaData.getProperty(key) == null) {
            providerMetaData.addProperty(new Property(key, properties.get(key)));
        }
    }

    private static void fillS3OldBinaryProviderInfo(ProviderMetaData providerMetaData, BinaryProviderConfig defaultValues) {
        addParam(providerMetaData, "verificationTimeout", defaultValues.getParam("s3VerificationTimeout"));
        addParam(providerMetaData, "useSignature", defaultValues.getParam("s3UseSignature"));
        addParam(providerMetaData, "multiPartLimit", defaultValues.getParam("s3MultiPartLimit"));
        addParam(providerMetaData, "awsVersion", defaultValues.getParam("s3AwsVersion"));
        addParam(providerMetaData, "region", defaultValues.getParam("s3Region"));
        addParam(providerMetaData, "identity", defaultValues.getParam("s3Identity"));
        addParam(providerMetaData, "credential", defaultValues.getParam("s3Credential"));
        addParam(providerMetaData, "bucketName", defaultValues.getParam("s3BucketName"));
        addParam(providerMetaData, "path", defaultValues.getParam("s3Path"));
        addParam(providerMetaData, "proxyIdentity", defaultValues.getParam("s3ProxyIdentity"));
        addParam(providerMetaData, "proxyCredential", defaultValues.getParam("s3ProxyCredential"));
        addParam(providerMetaData, "proxyPort", defaultValues.getParam("s3ProxyPort"));
        addParam(providerMetaData, "proxyHost", defaultValues.getParam("s3ProxyHost"));
        addParam(providerMetaData, "port", defaultValues.getParam("s3Port"));
        addParam(providerMetaData, "endpoint", defaultValues.getParam("s3Endpoint"));
        addParam(providerMetaData, "httpsOnly", defaultValues.getParam("s3HttpsOnly"));
        addParam(providerMetaData, "providerId", defaultValues.getParam("s3ProviderId"));
        addParam(providerMetaData, "httpsPort", defaultValues.getParam("s3HttpsPort"));
        Map<String, String> properties = defaultValues.getProperties();
        for (String key : properties.keySet()) {
            providerMetaData.addProperty(new Property(key, properties.get(key)));
        }
    }

    private static void fillShardBinaryProviderInfo(ProviderMetaData providerMetaData) {
        addParam(providerMetaData, "zone", "empty-zone");
        addParam(providerMetaData, "readBehavior", "roundRobin");
        addParam(providerMetaData, "writeBehavior", "roundRobin");
        addParam(providerMetaData, "redundancy", "1");
        addParam(providerMetaData, "concurrentStreamWaitTimeout", "30000");
        addParam(providerMetaData, "concurrentStreamBufferKb", "32");
        addParam(providerMetaData, "maxBalancingRunTime", "36000000");
        addParam(providerMetaData, "freeSpaceSampleInterval", "36000000");
        addParam(providerMetaData, "minSpareUploaderExecutor", "2");
        addParam(providerMetaData, "uploaderCleanupIdleTime", "120000");
    }

    private static void fillBlobBinaryProviderInfo() {

    }

    private static void addParam(ProviderMetaData metaData, String key, BinaryProviderConfig defaultValues) {
        if (metaData.getParam(key) == null) {
            String defaultValue = defaultValues.getParam(key);
            metaData.addParam(new Param(key, defaultValue));
        }
    }

    private static void addParam(ProviderMetaData metaData, String key, String defaultValue) {
        if (metaData.getParam(key) == null) {
            metaData.addParam(new Param(key, defaultValue));
        }
    }
}
