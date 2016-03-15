package org.artifactory.storage;

import java.io.IOException;
import java.io.InputStream;

import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.binstore.config.BinaryProviderConfigBuilder;
import org.artifactory.storage.binstore.config.model.ChainMetaData;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.artifactory.storage.binstore.config.BinaryProviderConfigBuilder.USER_TEMPLATE;


/**
 * @author Gidi Shabat
 */
// TODO [By Gidi]
@Test
public class BinaryProviderConfigTest {

    @Test
    public void userChainWithExistingProviderTest() throws IOException {
        InputStream userConfig = getInputStream("binarystore/config/binarystoreWithExistingProviders.xml");
        InputStream defaultConfig = getInputStream("default-storage-config.xml");
        BinaryProviderConfig defaultValues = getBinaryProviderDefaultValues();
        ChainMetaData chain = BinaryProviderConfigBuilder.buildByUserConfig(defaultConfig, userConfig, defaultValues);
        Assert.assertEquals(chain.getTemplate(), "full-db-direct", "Expecting user-chain");
        ProviderMetaData doubleProvider = chain.getProviderMetaData();
        Assert.assertEquals(doubleProvider.getType(), "blob", "Expecting providerMetaData type to be blob");
        Assert.assertEquals(doubleProvider.getId(), "blob", "Expecting providerMetaData id to be blob");
        Assert.assertNull(doubleProvider.getParamValue("useless"));
    }

    @Test
    public void userChainWithOverrideProviderTest() throws IOException {
        InputStream userConfig = getInputStream("binarystore/config/binarystoreWithOverideProviders.xml");
        InputStream defaultConfig = getInputStream("default-storage-config.xml");
        BinaryProviderConfig defaultValues = getBinaryProviderDefaultValues();
        ChainMetaData chain = BinaryProviderConfigBuilder.buildByUserConfig(defaultConfig, userConfig, defaultValues);
        Assert.assertEquals(chain.getTemplate(), "full-db-direct", "Expecting user-chain");
        ProviderMetaData doubleProvider = chain.getProviderMetaData();
        Assert.assertEquals(doubleProvider.getType(), "blob", "Expecting providerMetaData type to be blob");
        Assert.assertEquals(doubleProvider.getId(), "blob", "Expecting providerMetaData id to be blob");
        Assert.assertEquals(doubleProvider.getParamValue("useless"), "true");
    }

    @Test
    public void userChainConfigTest() throws IOException {
        InputStream userConfig = getInputStream("binarystore/config/binarystoreWithUserChain.xml");
        InputStream defaultConfig = getInputStream("default-storage-config.xml");
        BinaryProviderConfig defaultValues = getBinaryProviderDefaultValues();
        ChainMetaData chain = BinaryProviderConfigBuilder.buildByUserConfig(defaultConfig, userConfig, defaultValues);
        Assert.assertEquals(chain.getTemplate(), USER_TEMPLATE, "Expecting user-chain");
        ProviderMetaData cachefs = chain.getProviderMetaData();
        Assert.assertEquals(cachefs.getType(), "cache-fs", "Expecting providerMetaData type to be cache-fs");
        Assert.assertEquals(cachefs.getId(), "cache-fs", "Expecting providerMetaData id to be cache-fs");
        ProviderMetaData retry = cachefs.getProviderMetaData();
        Assert.assertEquals(retry.getType(), "retry", "Expecting providerMetaData type to be retry");
        Assert.assertEquals(retry.getId(), "retry", "Expecting providerMetaData id to be retry");
        ProviderMetaData fileSystem = retry.getProviderMetaData();
        Assert.assertEquals(fileSystem.getType(), "file-system", "Expecting providerMetaData type to be fileSystem");
        Assert.assertEquals(fileSystem.getId(), "file-system", "Expecting providerMetaData id to be fileSystem");
        Assert.assertEquals(fileSystem.getParamValue("fileStoreDir"), "test99", "Expecting override file system path");
        Assert.assertEquals(cachefs.getParamValue("cacheProviderDir"), "test89", "Expecting override file system path");
    }

    @Test
    public void templateTest() throws IOException {
        InputStream userConfig = getInputStream("binarystore/config/binarystoreWithTemplate.xml");
        InputStream defaultConfig = getInputStream("default-storage-config.xml");
        BinaryProviderConfig defaultValues = getBinaryProviderDefaultValues();
        ChainMetaData chain = BinaryProviderConfigBuilder.buildByUserConfig(defaultConfig, userConfig, defaultValues);
        Assert.assertEquals(chain.getTemplate(), "s3", "Expecting s3");
        ProviderMetaData cachefs = chain.getProviderMetaData();
        Assert.assertEquals(cachefs.getType(), "cache-fs", "Expecting providerMetaData type to be cache-fs");
        Assert.assertEquals(cachefs.getId(), "cache-fs", "Expecting providerMetaData id to be cache-fs");
        ProviderMetaData eventual = cachefs.getProviderMetaData();
        Assert.assertEquals(eventual.getType(), "eventual", "Expecting providerMetaData type to be eventual");
        Assert.assertEquals(eventual.getId(), "eventual", "Expecting providerMetaData id to be eventual");
        ProviderMetaData retry = eventual.getProviderMetaData();
        Assert.assertEquals(retry.getType(), "retry", "Expecting providerMetaData type to be retry");
        Assert.assertEquals(retry.getId(), "retry", "Expecting providerMetaData id to be retry");
        ProviderMetaData s3 = retry.getProviderMetaData();
        Assert.assertEquals(s3.getType(), "s3", "Expecting providerMetaData type to be s3");
        Assert.assertEquals(s3.getId(), "s3", "Expecting providerMetaData id to be s3");
        Assert.assertEquals(s3.getParams().size(), 18);
    }

    private BinaryProviderConfig getBinaryProviderDefaultValues() {
        BinaryProviderConfig binaryProviderConfig = new BinaryProviderConfig();
        binaryProviderConfig.addParam("fileStoreDir", "filestore");
        binaryProviderConfig.addParam("baseDataDir", "/test/data");
        binaryProviderConfig.addParam("cacheProviderDir", "filestore");
        return binaryProviderConfig;
    }

    @Test
    public void templateWithProviderTest() throws IOException {
        InputStream userConfig = getInputStream("binarystore/config/binarystore-s3-template.xml");
        InputStream defaultConfig = getInputStream("default-storage-config.xml");
        BinaryProviderConfig defaultValues = getBinaryProviderDefaultValues();
        ChainMetaData chain = BinaryProviderConfigBuilder.buildByUserConfig(defaultConfig, userConfig, defaultValues);
        Assert.assertEquals(chain.getTemplate(), "s3", "Expecting s3");
        ProviderMetaData cachefs = chain.getProviderMetaData();
        Assert.assertEquals(cachefs.getType(), "cache-fs", "Expecting providerMetaData type to be cache-fs");
        Assert.assertEquals(cachefs.getId(), "cache-fs", "Expecting providerMetaData id to be cache-fs");
        ProviderMetaData eventual = cachefs.getProviderMetaData();
        Assert.assertEquals(eventual.getType(), "eventual", "Expecting providerMetaData type to be eventual");
        Assert.assertEquals(eventual.getId(), "eventual", "Expecting providerMetaData id to be eventual");
        ProviderMetaData retry = eventual.getProviderMetaData();
        Assert.assertEquals(retry.getType(), "retry", "Expecting providerMetaData type to be retry");
        Assert.assertEquals(retry.getId(), "retry", "Expecting providerMetaData id to be retry");
        ProviderMetaData s3 = retry.getProviderMetaData();
        Assert.assertEquals(s3.getType(), "s3", "Expecting providerMetaData type to be s3");
        Assert.assertEquals(s3.getId(), "s3", "Expecting providerMetaData id to be s3");
        Assert.assertEquals(s3.getParams().size(), 18);
    }

    private InputStream getInputStream(String name) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResources(
                name).nextElement().openStream();
    }
}
