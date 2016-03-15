package org.artifactory.storage.db.binstore.service;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.storage.binstore.binary.providers.ExternalWrapperBinaryProviderImpl;
import org.artifactory.storage.binstore.binary.providers.ProviderConnectMode;
import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.binstore.ifc.BinaryProviderManager;
import org.artifactory.storage.binstore.service.BinaryData;
import org.artifactory.storage.db.binstore.dao.BinariesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author gidis
 */
public class ExternalBinaryProviderHelper {
    private static final Logger log = LoggerFactory.getLogger(ExternalBinaryProviderHelper.class);

    public static void disconnectFromFileStore(BinaryStoreImpl binaryStore, File externalDirFile,
                                               ProviderConnectMode disconnectMode, BasicStatusHolder statusHolder,
                                               BinaryProviderManager manager, BinariesDao binariesDao,
            BinaryProviderConfig defaultValues) {
        // The external binary provider works only if the file binary provider is not null
        if (binaryStore.getBinariesDir() == null) {
            return;
        }
        // First search for the external binary store to disconnect
        ExternalWrapperBinaryProviderImpl wrapper = manager.getExternalWrapperBinaryProvider(externalDirFile);
        String externalDir = externalDirFile.getAbsolutePath();
        if (wrapper != null) {
            wrapper.setConnectMode(disconnectMode);
        } else {
            String absolutePath = binaryStore.getBinariesDir().getAbsolutePath();
            manager.initializeExternalBinaryProvider(disconnectMode.propName, externalDir, absolutePath, defaultValues);
            wrapper = manager.getExternalWrapperBinaryProvider(externalDirFile);
        }

        // Now run fetch all on wrapper
        try {
            statusHolder.status("Disconnecting " + externalDir
                    + " using mode " + disconnectMode.propName, log);
            Collection<BinaryData> all = binariesDao.findAll();
            long sizeMoved = 0L;
            int total = all.size();
            int checked = 0;
            int done = 0;
            statusHolder.status("Found " + total + " files to disconnect!", log);
            for (BinaryData data : all) {
                try {
                    String sha1 = data.getSha1();
                    if (wrapper.connect(sha1)) {
                        statusHolder.debug("Activated " + disconnectMode.propName + " on " + sha1, log);
                        done++;
                        sizeMoved += data.getLength();
                    } else {
                        statusHolder.debug("File " + sha1 + " checked", log);
                    }
                    checked++;
                } catch (Exception e) {
                    statusHolder.error("Problem connecting checksum " + data, e, log);
                }
                if (checked % 200 == 0) {
                    statusHolder.status("Checked " + checked + "/" + total +
                            " files and disconnected " + done +
                            " total size " + disconnectMode.propName +
                            " is " + StorageUnit.toReadableString(sizeMoved), log);
                }
            }
            statusHolder.status("Checked " + checked + " files out of " + total +
                    " files and disconnected " + done +
                    " total size " + disconnectMode.propName +
                    " is " + StorageUnit.toReadableString(sizeMoved), log);
        } catch (SQLException e) {
            statusHolder.error("Could fetch all binary data from binary store", e, log);
        }
    }
}
