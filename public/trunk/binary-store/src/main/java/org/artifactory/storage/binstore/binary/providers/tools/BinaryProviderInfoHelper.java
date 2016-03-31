package org.artifactory.storage.binstore.binary.providers.tools;

import org.artifactory.storage.binstore.binary.providers.base.BinaryProviderBase;
import org.artifactory.storage.binstore.config.model.Param;
import org.artifactory.storage.binstore.config.model.Property;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;
import org.artifactory.storage.binstore.ifc.BinaryProviderInfo;

import static org.artifactory.storage.binstore.utils.MaskUtils.mask;

/**
 * @author Gidi Shabat
 */
public class BinaryProviderInfoHelper {

    public static BinaryProviderInfo fetchInfoFromBinary(BinaryProviderBase binaryProviderBase) {
        BinaryProviderInfo info = new BinaryProviderInfo();
        ProviderMetaData providerMetaData = binaryProviderBase.getProviderMetaData();
        for (Param param : providerMetaData.getParams()) {
            info.addProperty(param.getName(), param.getValue());
        }
        for (Property property : providerMetaData.getProperties()) {
            String name = property.getName();
            info.addProperty(name, mask(name, property.getValue()));
        }
        info.addProperty("type", providerMetaData.getType());
        info.addProperty("id", providerMetaData.getId());
        info.setStorageInfo(binaryProviderBase.getStorageInfo());
        return info;
    }
}