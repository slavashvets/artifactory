package org.artifactory.storage.db.binstore.visitors;

import org.artifactory.storage.binstore.binary.providers.base.StorageInfo;
import org.artifactory.storage.binstore.ifc.BinaryProviderInfo;
import org.artifactory.storage.binstore.ifc.model.BinaryTreeElement;

import java.util.Map;

/**
 * @author gidis
 */
public class EssentialBinaryTreeElementHandler implements BinaryTreeElementHandler<BinaryProviderInfo, Map<String, String>> {
    public static boolean isDisplayable(BinaryProviderInfo data) {
        String type = data.getProperties().get("type");
        switch (type) {
            case "empty": {
                return false;
            }
            case "eventual": {
                return false;
            }
            case "retry": {
                return false;
            }
            case "tracking": {
                return false;
            }
            case "external-wrapper": {
                return false;
            }
            default: {
                return true;
            }
        }
    }

    @Override
    public Map<String, String> visit(BinaryTreeElement<BinaryProviderInfo> binaryTreeElement) {
        BinaryProviderInfo data = binaryTreeElement.getData();
        boolean displayable = isDisplayable(data);
        if (displayable) {
            StorageInfo storageInfo = data.getStorageInfo();
            data.addProperty("freeSpace", toString(storageInfo.getFreeSpace()));
            data.addProperty("usageSpace", "" + toString(storageInfo.getUsageSpace()));
            data.addProperty("totalSpace", "" + toString(storageInfo.getTotalSpace()));
            data.addProperty("usageSpaceInPercent", "" + toString(storageInfo.getUsageSpaceInPercent()));
            data.addProperty("freeSpaceInPercent", "" + toString(storageInfo.getFreeSpaceInPercent()));
            return data.getProperties();
        }
        return null;
    }

    private String toString(Long number) {
        return Long.MAX_VALUE == number ? "infinite" : -1 == number ? "unsupported" : "" + number;
    }
}
