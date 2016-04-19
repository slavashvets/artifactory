package org.artifactory.storage.db.binstore.itest.service;

import java.io.File;
import java.io.IOException;

import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.StorageProperties.BinaryProviderType;
import org.artifactory.storage.binstore.binary.providers.EmptyBinaryProvider;
import org.artifactory.storage.binstore.binary.providers.FileBinaryProvider;
import org.artifactory.storage.binstore.binary.providers.FileBinaryProviderImpl;
import org.artifactory.storage.binstore.binary.providers.FileCacheBinaryProviderImpl;
import org.artifactory.storage.binstore.binary.providers.base.BinaryProviderBase;
import org.artifactory.storage.binstore.binary.providers.manager.BinaryProviderManagerImpl;
import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.binstore.ifc.BinaryProviderManager;
import org.artifactory.storage.db.binstore.service.BlobBinaryProviderImpl;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.artifactory.storage.StorageProperties.Key;
import static org.artifactory.util.ResourceUtils.getResourceAsFile;

/**
 * @author Gidi Shabat
 */
@Test
public class ConfigurableBinaryProviderManagerTest extends DbBaseTest {

    private BinaryProviderManager manager;
    @Autowired
    private StorageProperties storageProperties;

    @Test
    public void binaryProviderWithOverrideProviderTest() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        File configFile = getResourceAsFile("/binarystore/config/binarystoreWithOverideProviders.xml");
        defaultValues.setBinaryStoreXmlPath(configFile.getAbsolutePath());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase blobBinaryProvider = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(blobBinaryProvider instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blobBinaryProvider.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void binaryProviderWithTemplateTest() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        File configFile = getResourceAsFile("/binarystore/config/binarystore-filesystem-template.xml");
        defaultValues.setBinaryStoreXmlPath(configFile.getAbsolutePath());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase fileSystem = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("filestore", ((FileBinaryProvider) fileSystem).getBinariesDir().getName());
    }

    @Test
    public void binaryProviderWithExistingProviderTest() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        File configFile = getResourceAsFile("/binarystore/config/binarystoreWithExistingProviders.xml");
        defaultValues.setBinaryStoreXmlPath(configFile.getAbsolutePath());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase blobBinaryProvider = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(blobBinaryProvider instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blobBinaryProvider.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void binaryProviderWithTemplateAndProviderTest() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        File configFile = getResourceAsFile("/binarystore/config/binarystoreWithTemplateAndProvider.xml");
        defaultValues.setBinaryStoreXmlPath(configFile.getAbsolutePath());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase cache = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(cache instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase fileSystem = cache.next();
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("test654", ((FileBinaryProvider) fileSystem).getBinariesDir().getName());
    }

    @Test
    public void binaryProviderWithUserChainTest() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        File configFile = getResourceAsFile("/binarystore/config/binarystoreWithUserChain.xml");
        defaultValues.setBinaryStoreXmlPath(configFile.getAbsolutePath());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase cacheFs = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(cacheFs instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase retry = cacheFs.next();
        BinaryProviderBase fileSystem2 = retry.next();
        Assert.assertTrue(fileSystem2 instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem2.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("test89", ((FileBinaryProvider) cacheFs).getBinariesDir().getName());
        Assert.assertEquals("test99", ((FileBinaryProvider) fileSystem2).getBinariesDir().getName());
    }

    @Test
    public void oldGenerationWithFullDB() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        defaultValues.addParam("maxCacheSize", "1000");
        defaultValues.addParam("storageType", BinaryProviderType.fullDb.name());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase cacheFs = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(cacheFs instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase blob = cacheFs.next();
        Assert.assertTrue(blob instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blob.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("cache", ((FileBinaryProvider) cacheFs).getBinariesDir().getName());
    }

    @Test
    public void oldGenerationWithFullDBDirect() throws IOException {
        // If the cache size is 0 then no cache binary provider will be created
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        defaultValues.addParam("maxCacheSize", "0");
        defaultValues.addParam("storageType", BinaryProviderType.fullDb.name());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase blob = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(blob instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blob.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void oldGenerationWithFileSystemDBDirect() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        defaultValues.addParam("storageType", BinaryProviderType.filesystem.name());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase fileSystem = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void oldGenerationWithCacheAndFile() throws IOException {
        BinaryProviderConfig defaultValues = storageProperties.toDefaultValues();
        defaultValues.addParam("storageType", BinaryProviderType.cachedFS.name());
        manager = new BinaryProviderManagerImpl(defaultValues);
        BinaryProviderBase cacheFS = (BinaryProviderBase) ReflectionTestUtils.getField(manager,
                "firstBinaryProvider");
        Assert.assertTrue(cacheFS instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase fileSystem = cacheFS.next();
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    protected void updateStorageProperty(Key key, String value) {
        Object propsField = ReflectionTestUtils.getField(storageProperties, "props");
        ReflectionTestUtils.invokeMethod(propsField, "setProperty", key.key(), value);
    }
}
