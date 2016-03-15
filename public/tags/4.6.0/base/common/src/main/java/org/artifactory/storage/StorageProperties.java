/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.storage;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.db.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * A convenient class to parse the storage properties file.
 *
 * @author Yossi Shaul
 */
public class StorageProperties {
    protected static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 100;
    protected static final int DEFAULT_MAX_IDLE_CONNECTIONS = 10;
    private static final Logger log = LoggerFactory.getLogger(StorageProperties.class);
    private static final String DEFAULT_MAX_CACHE_SIZE = "5GB";

    private LinkedProperties props = null;
    private DbType dbType = null;

    public StorageProperties(File storagePropsFile) throws IOException {
        props = new LinkedProperties();
        try (FileInputStream pis = new FileInputStream(storagePropsFile)) {
            props.load(pis);
        }

        trimValues();
        assertMandatoryProperties();

        // cache commonly used properties
        dbType = DbType.parse(getProperty(Key.type));

        // verify that the database is supported (will throw an exception if not found)
        log.debug("Loaded storage properties for supported database type: {}", getDbType());
    }

    /**
     * update storage properties file;
     *
     * @throws IOException
     */
    public void updateStoragePropertiesFile(File updateStoragePropFile) throws IOException {
        if (props != null) {
            OutputStream outputStream = new FileOutputStream(updateStoragePropFile);
            props.store(outputStream, "");
        }
    }

    public DbType getDbType() {
        return dbType;
    }

    public String getConnectionUrl() {
        return getProperty(Key.url);
    }

    /**
     * Update the connection URL property (should only be called for derby when the url contains place holders)
     *
     * @param connectionUrl The new connection URL
     */
    public void setConnectionUrl(String connectionUrl) {
        props.setProperty(Key.url.key, connectionUrl);
    }

    public String getDriverClass() {
        return getProperty(Key.driver);
    }

    public String getUsername() {
        return getProperty(Key.username);
    }

    public String getPassword() {
        String password = getProperty(Key.password);
        password = CryptoHelper.decryptIfNeeded(password);
        return password;
    }

    public void setPassword(String updatedPassword) {
        props.setProperty(Key.password.key, updatedPassword);
    }

    public String getTempDir() {
        return getProperty(Key.binaryProviderFilesystemTempDir, "_pre");
    }

    public int getMaxActiveConnections() {
        return getIntProperty(Key.maxActiveConnections.key, DEFAULT_MAX_ACTIVE_CONNECTIONS);
    }

    public int getMaxIdleConnections() {
        return getIntProperty(Key.maxIdleConnections.key, DEFAULT_MAX_IDLE_CONNECTIONS);
    }

    @Nonnull
    public BinaryProviderType getBinariesStorageType() {
        return BinaryProviderType.valueOf(
                getProperty(Key.binaryProviderType, BinaryProviderType.filesystem.name()));
    }

    public String getS3BucketName() {
        return getProperty(Key.binaryProviderS3BucketName, null);
    }

    public String getS3Credential() {
        String credential = getProperty(Key.binaryProviderS3Credential);
        return CryptoHelper.decryptIfNeeded(credential);
    }

    public void setS3Credential(String credential) {
        props.setProperty(Key.binaryProviderS3Credential.key(), credential);
    }

    public Map<String, String> getS3Params() {
        return getProperties(Key.binaryProviderS3Param.key + ".");
    }

    public Map<String, String> getGCParams() {
        return getProperties(Key.binaryProviderGCParam + ".");
    }

    public String getS3ProxyCredential() {
        String credential = getProperty(Key.binaryProviderS3ProxyCredential.key, null);
        return CryptoHelper.decryptIfNeeded(credential);
    }

    public void setS3ProxyCredential(String credential) {
        props.setProperty(Key.binaryProviderS3ProxyCredential.key(), credential);
    }

    public String getBinaryProviderExternalDir() {
        return getProperty(Key.binaryProviderExternalDir);
    }

    public long getCacheProviderMaxSize() {
        return StorageUnit.fromReadableString(getProperty(Key.binaryProviderCacheMaxSize, DEFAULT_MAX_CACHE_SIZE));
    }

    public String getProperty(Key property) {
        return props.getProperty(property.key);
    }

    public String getProperty(Key property, String defaultValue) {
        return props.getProperty(property.key, defaultValue);
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, defaultValue + ""));
    }

    public int getIntProperty(String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, defaultValue + ""));
    }

    public long getLongProperty(String key, long defaultValue) {
        return Long.parseLong(props.getProperty(key, defaultValue + ""));
    }

    public Map<String, String> getProperties(String prefix) {
        Map<String, String> result = Maps.newHashMap();
        Iterator<Map.Entry<String, String>> iterator = props.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            if (next.getKey().startsWith(prefix)) {
                String reminder = next.getKey().replace(prefix, "");
                if (!StringUtils.isBlank(reminder)) {
                    result.put(reminder, next.getValue());
                }
            }
        }
        return result;
    }

    private void trimValues() {
        Iterator<Map.Entry<String, String>> iter = props.iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            String value = entry.getValue();
            if (!StringUtils.trimToEmpty(value).equals(value)) {
                entry.setValue(StringUtils.trim(value));
            }
        }
    }

    private void assertMandatoryProperties() {
        Key[] mandatory = {Key.type, Key.url, Key.driver};
        for (Key mandatoryProperty : mandatory) {
            String value = getProperty(mandatoryProperty);
            if (StringUtils.isBlank(value)) {
                throw new IllegalStateException("Mandatory storage property '" + mandatoryProperty + "' doesn't exist");
            }
        }
    }

    public boolean isDerby() {
        return dbType == DbType.DERBY;
    }

    public boolean isPostgres() {
        return dbType == DbType.POSTGRESQL;
    }

    public BinaryProviderConfig toDefaultValues() {
        BinaryProviderConfig defaultValues = new BinaryProviderConfig();
        for (Key key : Key.values()) {
            if (!key.isBinaryProviderField()) {
                continue;
            }
            if (Key.binaryProviderCacheMaxSize.equals(key)) {
                String value = getProperty(key, DEFAULT_MAX_CACHE_SIZE);
                defaultValues.addParam(key.signature, "" + StorageUnit.fromReadableString(value));
            } else {
                String defaultValue = key.getDefaultValue() != null ? key.getDefaultValue().toString() : null;
                defaultValues.addParam(key.signature, getProperty(key.key(), defaultValue));
            }
        }
        defaultValues.addParam("baseDataDir", ArtifactoryHome.get().getHaAwareDataDir().getAbsolutePath());
        addS3Properties(defaultValues);
        addGCProperties(defaultValues);
        return defaultValues;
    }

    private void addS3Properties(BinaryProviderConfig binaryProviderConfig) {
        Map<String, String> s3Params = getS3Params();
        for (String key : s3Params.keySet()) {
            binaryProviderConfig.addProperty(key, s3Params.get(key));
        }
    }

    private void addGCProperties(BinaryProviderConfig binaryProviderConfig) {
        Map<String, String> s3Params = getGCParams();
        for (String key : s3Params.keySet()) {
            binaryProviderConfig.addProperty(key, s3Params.get(key));
        }
    }


    public enum Key {
        username, password, type, url, driver,
        maxActiveConnections("pool.max.active", null, null),
        maxIdleConnections("pool.max.idle", null, null),

        binaryProviderType("binary.provider.type", "storageType", BinaryProviderType.filesystem.name()),  // see BinaryProviderType
        binaryProviderCacheMaxSize("binary.provider.cache.maxSize", "maxCacheSize", DEFAULT_MAX_CACHE_SIZE),
        binaryProviderCacheCacheSynchQuitePeriod("binary.provider.cache.synch.quite.period", "cacheSynchQuietPeriod", 60 * 1000),
        binaryProviderCacheDir("binary.provider.cache.dir", "cacheProviderDir", "cache"),
        binaryProviderFilesystemDir("binary.provider.filesystem.dir", "fileStoreDir", "filestore"),
        binaryProviderFilesystemTempDir("binary.provider.filesystem.temp.dir", "tempDir", "_pre"),
        binaryProviderExternalDir("binary.provider.external.dir", "externalDir", null),
        binaryProviderExternalMode("binary.provider.external.mode", "connectMode", null),
        binaryProviderInfoEvictionTime("binary.provider.info.eviction.time", "binaryProviderInfoEvictionTime", 1000 * 5),

        // Retry binary provider
        binaryProviderRetryMaxRetriesNumber("binary.provider.retry.max.retries.number", "maxTrays", 5),
        binaryProviderRetryDelayBetweenRetries("binary.provider.retry.delay.between.retries", "interval", 5000),

        // S3 binary provider
        binaryProviderS3Identity("binary.provider.s3.identity", "s3Identity", null),
        binaryProviderS3UseSignature("binary.provider.s3.use.signature", "s3UseSignature", false),
        binaryProviderS3Credential("binary.provider.s3.credential", "s3Credential", null),
        binaryProviderS3BlobVerifyTimeout("binary.provider.s3.blob.verification.timeout", "s3VerificationTimeout", 60000),
        binaryProviderS3ProxyIdentity("binary.provider.s3.proxy.identity", "s3ProxyIdentity", null),
        binaryProviderS3ProxyCredential("binary.provider.s3.proxy.credential", "s3ProxyCredential", null),
        binaryProviderS3BucketName("binary.provider.s3.bucket.name", "s3BucketName", null),
        binaryProviderS3BucketPath("binary.provider.s3.bucket.path", "s3Path", "filestore"),
        binaryProviderS3ProviderId("binary.provider.s3.provider.id", "s3ProviderId", "s3"),
        binaryProviderS3Endpoint("binary.provider.s3.endpoint", "s3Endpoint", null),
        binaryProviderS3EndpointPort("binary.provider.s3.endpoint.port", "s3Port", -1),
        binaryProviderS3awsVersion("binary.provider.s3.aws.version", "s3Version", null),
        binaryProviderS3Region("binary.provider.s3.region", "s3Region", null),
        binaryProviderS3MultiPartLimit("binary.provider.s3.multi.part.limit", "s3MultiPartLimit", 100 * 1000 * 1000),
        binaryProviderS3HttpsOnly("binary.provider.s3.https.only", "s3HttpsOnly", true),
        binaryProviderS3HttpsPort("binary.provider.s3.https.only", "s3HttpsPort", -1),
        binaryProviderS3TestConnection("binary.provider.s3.test.connection", "s3TestConnection", true),
        binaryProviderS3ProxyPort("binary.provider.s3.proxy.port", "s3ProxyPort", -1),
        binaryProviderS3ProxyHost("binary.provider.s3.proxy.host", "s3ProxyHost", null),

        // Google binary provider
        binaryProviderGsIdentity("binary.provider.gs.identity", "gsIdentity", null),
        binaryProviderGsCredential("binary.provider.gs.credential", "gsCredential", null),
        binaryProviderGsProxyIdentity("binary.provider.gs.proxy.identity", "gsProxyIdentity", null),
        binaryProviderGsProxyCredential("binary.provider.gs.proxy.credential", "gsProxyCredential", null),
        binaryProviderGsBucketName("binary.provider.gs.bucket.name", "gsBucketName", null),
        binaryProviderGsBucketPath("binary.provider.gs.bucket.path", "gsPath", "filestore"),
        binaryProviderGsHttpPort("binary.provider.gs.endpoint.http.port", "gsPort", 80),
        binaryProviderGsEndPoint("binary.provider.gs.endpoint", "gsEndpoint", "commondatastorage.googleapis.com"),
        binaryProviderGsHttpsOnly("binary.provider.gs.https.only", "gsHttpsOnly", true),
        binaryProviderGsHttpsPort("binary.provider.gs.https.port", "gsHttpsPort", 443),
        binaryProviderGsTestConnection("binary.provider.gs.test.connection", "gsTestConnection", true),
        binaryProviderGsProxyPort("binary.provider.gs.proxy.port", "gsProxyPort", -1),
        binaryProviderGsProxyHost("binary.provider.gs.proxy.host", "gsProxyHost", null),

        // Dynamic S3 Param
        binaryProviderS3Param("binary.provider.s3.env", null, null),

        // Dynamic GC Param
        binaryProviderGCParam("binary.provider.gc.env", null, null),

        // Eventually persisted binary provider
        binaryProviderEventuallyPersistedMaxNumberOfTreads(
                "binary.provider.eventually.persisted.max.number.of.threads", "numberOfThreads", 5),
        binaryProviderEventuallyPersistedTimeOut("binary.provider.eventually.persisted.timeout", "timeout", 120000),
        binaryProviderEventuallyPersistedDispatcherSleepTime(
                "binary.provider.eventually.dispatcher.sleep.time", "dispatcherInterval", 5000), // in millis
        binaryProviderEventuallyPersistedWaitHazelcastTime(
                "binary.provider.eventually.persisted.wait.hazelcast.time", "hazelcastWaitingTime", 5000), // in millis
        binaryProviderEventuallyPersistedQueueSize(
                "binary.provider.eventually.persisted.queue.size", "queueSize", 64);

        private final String key;
        private final Object defaultValue;
        private final String signature;

        Key() {
            this.key = name();
            this.defaultValue = null;
            this.signature = null;
        }

        Key(String key, String signature, Object defaultValue) {
            this.key = key;
            this.signature = signature;
            this.defaultValue = defaultValue;
        }

        public String key() {
            return key;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public boolean isBinaryProviderField() {
            return !(this.equals(username) || this.equals(password) || this.equals(type) || this.equals(url) ||
                    this.equals(driver) || this.equals(maxActiveConnections) || this.equals(maxIdleConnections)
                    || this.equals(binaryProviderS3Param) || this.equals(binaryProviderGCParam));
        }
    }

    public enum BinaryProviderType {
        filesystem, // binaries are stored in the filesystem
        fullDb,     // binaries are stored as blobs in the db, filesystem is used for caching unless cache size is 0
        cachedFS,   // binaries are stored in the filesystem, but a front cache (faster access) is added
        S3,         // binaries are stored in S3 JClouds API
        S3Old,        // binaries are stored in S3 Jets3t API
        goog        // binaries are stored in S3 Jets3t API
    }
}
