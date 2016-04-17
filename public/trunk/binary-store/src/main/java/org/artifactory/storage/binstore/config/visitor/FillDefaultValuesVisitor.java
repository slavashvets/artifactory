package org.artifactory.storage.binstore.config.visitor;

import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.binstore.config.BinaryProviderMetaDataFiller;
import org.artifactory.storage.binstore.config.model.ChainMetaData;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;

/**
 * @author Gidi Shabat
 */
public class FillDefaultValuesVisitor extends ConfigVisitor {


    private BinaryProviderConfig defaultValues;

    public FillDefaultValuesVisitor(BinaryProviderConfig defaultValues) {
        this.defaultValues = defaultValues;
    }

    @Override
    ProviderMetaData onProvider(ProviderMetaData providerMetaData) {
        return null;
    }

    @Override
    ChainMetaData onChain(ChainMetaData source) {
        return null;
    }

    @Override
    ProviderMetaData onChainSubProvider(ProviderMetaData providerMetaData) {
        BinaryProviderMetaDataFiller.fillMetaData(providerMetaData,defaultValues);
        return null;
    }

    @Override
    public ProviderMetaData onChainProvider(ProviderMetaData providerMetaData) {
        BinaryProviderMetaDataFiller.fillMetaData(providerMetaData,defaultValues);
        return null;
    }
}
