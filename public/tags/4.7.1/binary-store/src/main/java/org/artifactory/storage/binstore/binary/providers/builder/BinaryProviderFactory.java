package org.artifactory.storage.binstore.binary.providers.builder;

import com.google.common.collect.Lists;
import org.artifactory.storage.binstore.binary.providers.EmptyBinaryProvider;
import org.artifactory.storage.binstore.binary.providers.FileBinaryProvider;
import org.artifactory.storage.binstore.binary.providers.base.BinaryProviderBase;
import org.artifactory.storage.binstore.binary.providers.base.BinaryProviderServices;
import org.artifactory.storage.binstore.binary.providers.base.MutableBinaryProvider;
import org.artifactory.storage.binstore.config.model.ChainMetaData;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.artifactory.storage.binstore.binary.providers.base.MutableBinaryProviderInjector.getMutableBinaryProvider;
import static org.artifactory.storage.binstore.binary.providers.base.MutableBinaryProviderInjector.setMutableBinaryProvider;

/**
 * @author Gidi Shabat
 */
public class BinaryProviderFactory {
    private static final Logger log = LoggerFactory.getLogger(BinaryProviderFactory.class);


    public static <T extends BinaryProviderBase> T buildProvider(ProviderMetaData providerMetaData, BinaryProviderServices binaryStore) {
        BinaryProviderBase binaryProviderBase = create(providerMetaData, binaryStore);
        binaryProviderBase.initialize();
        return (T) binaryProviderBase;
    }

    private static <T extends BinaryProviderBase> T create(ProviderMetaData providerMetaData, BinaryProviderServices binaryStore) {
        try {
            Map<String, Class> binaryProvidersMap = binaryStore.getBinaryProvidersMap();
            Class binaryProviderClass = binaryProvidersMap.get(providerMetaData.getType());
            BinaryProviderBase instance = (BinaryProviderBase) binaryProviderClass.newInstance();
            MutableBinaryProvider mutableBinaryProvider = new MutableBinaryProvider();
            mutableBinaryProvider.setProviderMetaData(providerMetaData);
            mutableBinaryProvider.setBinaryProviderServices(binaryStore);
            EmptyBinaryProvider empty = new EmptyBinaryProvider();
            MutableBinaryProvider mutableEmptyBinaryProvider = new MutableBinaryProvider();
            mutableEmptyBinaryProvider.setProviderMetaData(new ProviderMetaData("empty", "empty"));
            setMutableBinaryProvider(empty, mutableEmptyBinaryProvider);
            mutableBinaryProvider.setEmpty(empty);
            setMutableBinaryProvider(instance, mutableBinaryProvider);
            return (T) instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate binary provider.", e);
        }
    }

    public static BinaryProviderBase buildProviders(ChainMetaData configChain, BinaryProviderServices binaryStore) {
        log.debug("Initializing providers by chain; '{}'", configChain.getTemplate());
        List<BinaryProviderBase> binaryProviders = Lists.newArrayList();
        binaryProviders.add(build(configChain.getProviderMetaData(), binaryStore));
        return binaryProviders.get(0);
    }

    private static BinaryProviderBase build(ProviderMetaData providerMetaData, BinaryProviderServices binaryStore) {
        if (providerMetaData == null) {
            return null;
        }
        BinaryProviderBase binaryProviderBase = create(providerMetaData, binaryStore);
        MutableBinaryProvider mutableBinaryProvider = getMutableBinaryProvider(binaryProviderBase);
        BinaryProviderBase next = build(providerMetaData.getProviderMetaData(), binaryStore);
        mutableBinaryProvider.setBinaryProvider(next);
        if (next != null) {
            MutableBinaryProvider childMutableBinaryProvider = getMutableBinaryProvider(next);
            childMutableBinaryProvider.setParentBinaryProvider(binaryProviderBase);
        }
        for (ProviderMetaData subProviderMetaData : providerMetaData.getSubProviderMetaDataList()) {
            BinaryProviderBase subBinaryProvider = build(subProviderMetaData, binaryStore);
            mutableBinaryProvider.addSubBinaryProvider(subBinaryProvider);
            MutableBinaryProvider childMutableSubBinaryProvider = getMutableBinaryProvider(subBinaryProvider);
            childMutableSubBinaryProvider.setParentBinaryProvider(binaryProviderBase);
        }
        binaryProviderBase.initialize();
        return binaryProviderBase;
    }


    public static FileBinaryProvider searchForFileBinaryProvider(BinaryProviderBase binaryProvider) {
        if (binaryProvider == null) {
            return null;
        }
        if (binaryProvider instanceof FileBinaryProvider) {
            return (FileBinaryProvider) binaryProvider;
        }
        FileBinaryProvider fileBinaryProvider = searchForFileBinaryProvider(binaryProvider.getBinaryProvider());
        if (fileBinaryProvider != null) {
            return fileBinaryProvider;
        }
        for (BinaryProviderBase binaryProviderBase : binaryProvider.getSubBinaryProviders()) {
            if (binaryProviderBase instanceof FileBinaryProvider) {
                return (FileBinaryProvider) binaryProviderBase;
            } else {
                fileBinaryProvider = searchForFileBinaryProvider(binaryProviderBase);
                if (fileBinaryProvider != null) {
                    return fileBinaryProvider;
                }
            }
        }
        return null;
    }

}
