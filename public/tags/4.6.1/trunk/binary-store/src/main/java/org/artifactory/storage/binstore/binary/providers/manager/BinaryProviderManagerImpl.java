package org.artifactory.storage.binstore.binary.providers.manager;

import com.google.common.collect.Lists;
import org.artifactory.storage.binstore.binary.providers.ExternalFileBinaryProviderImpl;
import org.artifactory.storage.binstore.binary.providers.ExternalWrapperBinaryProviderImpl;
import org.artifactory.storage.binstore.binary.providers.FileBinaryProvider;
import org.artifactory.storage.binstore.binary.providers.StateAwareBinaryProvider;
import org.artifactory.storage.binstore.binary.providers.base.*;
import org.artifactory.storage.binstore.binary.providers.builder.BinaryProviderFactory;
import org.artifactory.storage.binstore.binary.providers.tools.BinaryProviderClassScanner;
import org.artifactory.storage.binstore.binary.providers.tools.BinaryProviderInfoHelper;
import org.artifactory.storage.binstore.config.BinaryProviderConfig;
import org.artifactory.storage.binstore.config.BinaryProviderMetaDataFiller;
import org.artifactory.storage.binstore.config.model.ChainMetaData;
import org.artifactory.storage.binstore.config.model.Param;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;
import org.artifactory.storage.binstore.exceptions.BinaryNotFoundException;
import org.artifactory.storage.binstore.exceptions.StorageException;
import org.artifactory.storage.binstore.ifc.BinaryProviderInfo;
import org.artifactory.storage.binstore.ifc.BinaryProviderManager;
import org.artifactory.storage.binstore.ifc.model.BinaryTreeElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.artifactory.storage.binstore.binary.providers.base.MutableBinaryProviderInjector.getMutableBinaryProvider;
import static org.artifactory.storage.binstore.binary.providers.builder.BinaryProviderFactory.buildProviders;
import static org.artifactory.storage.binstore.binary.providers.builder.BinaryProviderFactory.searchForFileBinaryProvider;
import static org.artifactory.storage.binstore.config.ConfigurableBinaryProviderManager.buildByConfig;
import static org.artifactory.storage.binstore.config.ConfigurableBinaryProviderManager.buildByStorageProperties;

/**
 * @author Gidi Shabat
 */
public class BinaryProviderManagerImpl implements BinaryProviderServices, BinaryProviderManager {
    private static final Logger log = LoggerFactory.getLogger(BinaryProviderManagerImpl.class);
    private final BinaryProviderBase firstBinaryProvider;
    private final FileBinaryProvider fileBinaryProvider;
    private final Map<String, Class> binaryProvidersMap;
    private final long binaryProvidersInfoEvictionTime;
    private long lastBinaryProvidersInfoRequestTime;
    private BinaryTreeElement<BinaryProviderInfo> lastBinaryProvidersInfoResult;

    private Queue<String> errors = new ConcurrentLinkedDeque<>();
    private boolean needOptimization = false;

    public BinaryProviderManagerImpl(BinaryProviderConfig storageConfig) {
        try {
            log.debug("Initializing the ConfigurableBinaryProviderManager");
            String evictionTimeString = storageConfig.getParam("binaryProviderInfoEvictionTime");
            binaryProvidersInfoEvictionTime = Long.parseLong(evictionTimeString);
            binaryProvidersMap = BinaryProviderClassScanner.loadProvidersMap();

            // If the new generation binary config exist the use it else use the old generation filestore
            String binaryStoreXmlPath = storageConfig.getBinaryStoreXmlPath();
            String configFile = binaryStoreXmlPath != null ? binaryStoreXmlPath : "binarystore.xml";
            File userConfigFile = new File(configFile);
            ChainMetaData selectedChain;
            if (userConfigFile.exists()) {
                // Create binary provider according to the The new generation config
                selectedChain = buildByConfig(userConfigFile, storageConfig);
            } else {
                // Create binary provider using to the The old generation properties
                selectedChain = buildByStorageProperties(storageConfig);
            }

            firstBinaryProvider = buildProviders(selectedChain, this);
            fileBinaryProvider = searchForFileBinaryProvider(firstBinaryProvider);
            addExternalBinaryProviders(storageConfig);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to initialize binary providers. Reason io exception occurred during the config read process");
        }
    }

    private void addExternalBinaryProviders(BinaryProviderConfig storageConfig) {
        // Add External binary providers
        String mode = storageConfig.getParam("connectMode");
        String externalDir = storageConfig.getParam("externalDir");
        String fileStoreDir = storageConfig.getParam("fileStoreDir");
        File fileStoreFullPath = new File(new File(storageConfig.getParam("baseDataDir")), fileStoreDir);
        String absolutePath = fileStoreFullPath.getAbsolutePath();
        initializeExternalBinaryProvider(mode, externalDir, absolutePath, storageConfig);
    }

    public void initializeExternalBinaryProvider(String mode, String externalDir, String absolutePath,
            BinaryProviderConfig defaultValues) {
        addBinaryProvider(createExternalBinaryProviders(mode, externalDir, absolutePath, defaultValues));
    }

    private void addBinaryProvider(List<ProviderMetaData> metaDatas) {
        List<BinaryProviderBase> newProviders = Lists.newArrayList();
        for (ProviderMetaData metaData : metaDatas) {
            newProviders.add(BinaryProviderFactory.buildProvider(metaData, this));
        }
        BinaryProviderBase binaryProvider = (BinaryProviderBase) fileBinaryProvider;
        if (binaryProvider != null) {
            while (binaryProvider.getBinaryProvider() != null) {
                binaryProvider = binaryProvider.getBinaryProvider();
            }
            for (BinaryProviderBase toAdd : newProviders) {
                MutableBinaryProvider mutableBinaryProvider = getMutableBinaryProvider(binaryProvider);
                mutableBinaryProvider.setBinaryProvider(toAdd);
                binaryProvider = toAdd;
            }
        }
    }

    @Override
    public void notifyError(String id, String type) {
        needOptimization = true;
        errors.add("id: " + id + " type: " + type);
    }

    @Override
    public Map<String, Class> getBinaryProvidersMap() {
        return binaryProvidersMap;
    }

    private boolean exists(String sha1) {
        return firstBinaryProvider.exists(sha1);
    }

    private BinaryInfo addStream(BinaryStream binaryStream) throws IOException {
        return firstBinaryProvider.addStream(binaryStream);
    }

    private InputStream getStream(String sha1) {
        return firstBinaryProvider.getStream(sha1);
    }

    @Override
    public BinaryTreeElement<BinaryProviderInfo> getBinaryProvidersInfo() {
        long cacheAge = System.currentTimeMillis() - lastBinaryProvidersInfoRequestTime;
        if (lastBinaryProvidersInfoResult == null || cacheAge > binaryProvidersInfoEvictionTime) {
            lastBinaryProvidersInfoResult = firstBinaryProvider.visit(BinaryProviderInfoHelper::fetchInfoFromBinary);
            lastBinaryProvidersInfoRequestTime = System.currentTimeMillis();
        }
        return lastBinaryProvidersInfoResult;
    }

    //TODO: [by YS] ping should be adjusted to support other binary providers
    @Override
    public void ping() {
        FileBinaryProvider binaryProvider = fileBinaryProvider;
        if (binaryProvider != null) {
            if (!binaryProvider.isAccessible()) {
                throw new StorageException("Cannot access " + binaryProvider.getBinariesDir().getAbsolutePath());
            }
        }
    }

    private boolean delete(String sha1) {
        return firstBinaryProvider.delete(sha1);
    }

    private StorageInfo getStorageInfo() {
        return firstBinaryProvider.getStorageInfo();
    }

    @Override
    public ObjectInputStream getBinariesDataStream() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        ObjectOutputStream outputStream = new ObjectOutputStream(out);
        new Thread(() -> {
            try {
                firstBinaryProvider.collect(new FileListCollector(outputStream));
                outputStream.writeObject(null); // mark finished
            } catch (IOException e) {
                log.error("Failed stream binaries list", e.getMessage());
            }
        }).start();
        return new ObjectInputStream(in);
    }

    @Override
    public boolean optimize(boolean force) {
        if (!force && !needOptimization) {
            return false;
        }
        List<String> inactives = Lists.newArrayList();
        firstBinaryProvider.collect(new ReactivateBinaryProviderCollector(inactives));
        // if We Found inactive provider then print it and return, there is no need for balancing.
        if (!force && inactives.size() > 0) {
            log.debug("Found inactive providers can not execute optimization.");
            for (String id : inactives) {
                log.error("Inactive and unrecoverable binary provider with id: " + id + " found skipping the balance " +
                        "process please check logs for more details");
            }
            return false;
        } else {
            firstBinaryProvider.collect(new OptimizationBinaryProviderVisitor());
            needOptimization = false;
            return true;
        }
    }

    @Override
    public List<String> getErrors() {
        ArrayList<String> result = Lists.newArrayList();
        while (!errors.isEmpty()) {
            result.add(errors.remove());
        }
        return result;
    }

    @Override
    public ExternalWrapperBinaryProviderImpl getExternalWrapperBinaryProvider(File externalDir) {
        BinaryProviderBase bp = firstBinaryProvider;
        FileBinaryProvider externalFilestore = null;
        while (bp != null) {
            if (bp instanceof ExternalFileBinaryProviderImpl
                    && ((ExternalFileBinaryProviderImpl) bp).getBinariesDir().getAbsolutePath()
                    .equals(externalDir.getAbsolutePath())) {
                externalFilestore = (ExternalFileBinaryProviderImpl) bp;
                break;
            }
            bp = bp.next();
        }
        if (externalFilestore == null) {
            log.error("Could not find any external filestore" +
                    " pointing to " + externalDir.getAbsolutePath());
            return null;
        }

        // Then look for wrapper if exists
        ExternalWrapperBinaryProviderImpl wrapper = null;
        bp = firstBinaryProvider;
        while (bp != null) {
            if (bp instanceof ExternalWrapperBinaryProviderImpl) {
                if (((ExternalWrapperBinaryProviderImpl) bp).nextFileProvider() == externalFilestore) {
                    wrapper = (ExternalWrapperBinaryProviderImpl) bp;
                    break;
                }
            }
            bp = bp.next();
        }
        return wrapper;
    }

    @Override
    public BinaryProvider getBinaryProvider() {
        return new BinaryProvider() {

            @Override
            public boolean exists(String sha1) {
                return BinaryProviderManagerImpl.this.exists(sha1);
            }

            @Nonnull
            @Override
            public InputStream getStream(String sha1) throws BinaryNotFoundException {
                return BinaryProviderManagerImpl.this.getStream(sha1);
            }

            @Override
            public BinaryInfo addStream(BinaryStream binaryStream) throws IOException {
                return BinaryProviderManagerImpl.this.addStream(binaryStream);
            }

            @Override
            public boolean delete(String sha1) {
                return BinaryProviderManagerImpl.this.delete(sha1);
            }

            @Nonnull
            @Override
            public StorageInfo getStorageInfo() {
                return BinaryProviderManagerImpl.this.getStorageInfo();
            }
        };
    }

    private List<ProviderMetaData> createExternalBinaryProviders(String mode, String externalDir,
            String fileStoreFullPath, BinaryProviderConfig defaultValues) {
        List<ProviderMetaData> result = Lists.newArrayList();
        if (externalDir != null) {
            if (mode != null) {
                ProviderMetaData externalWrapperBinaryProvider = createExternalWrapperBinaryProvider(mode, fileStoreFullPath);
                BinaryProviderMetaDataFiller.fillMetaData(externalWrapperBinaryProvider, defaultValues);
                result.add(externalWrapperBinaryProvider);
            }
            ProviderMetaData externalFileBinaryProvider = createExternalFileBinaryProvider(externalDir);
            BinaryProviderMetaDataFiller.fillMetaData(externalFileBinaryProvider, defaultValues);
            result.add(externalFileBinaryProvider);
        }
        return result;
    }

    private ProviderMetaData createExternalWrapperBinaryProvider(String mode, String dir) {
        ProviderMetaData providerMetaData = new ProviderMetaData("external-wrapper", "external-wrapper");
        providerMetaData.addParam(new Param("connectMode", mode));
        providerMetaData.addParam(new Param("fileStoreDir", dir));
        return providerMetaData;
    }

    public ProviderMetaData createExternalFileBinaryProvider(String dir) {
        ProviderMetaData providerMetaData = new ProviderMetaData("external-file", "external-file");
        providerMetaData.addParam(new Param("externalDir", dir));
        return providerMetaData;
    }

    private static class FileListCollector implements BinaryProviderCollector {
        private ObjectOutputStream outputStream;

        public FileListCollector(ObjectOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void collect(BinaryProviderBase binaryProviderBase) {
            if (binaryProviderBase instanceof ExternalWrapperBinaryProviderImpl) {
                return;
            }
            if (binaryProviderBase instanceof ExternalFileBinaryProviderImpl) {
                return;
            }
            if (binaryProviderBase instanceof FileBinaryProvider) {
                //TODO: [by YS] implementation leak of binary provider - this should be done similar to the OptimizationBinaryProviderVisitor
                File binariesDir = ((FileBinaryProvider) binaryProviderBase).getBinariesDir();
                if (binariesDir.list() == null) {
                    return;
                }
                for (String directoryName : binariesDir.list()) {
                    if ("_pre".equals(directoryName)) {
                        continue;
                    }
                    File subDir = new File(binariesDir, directoryName);
                    for (String fileName : subDir.list()) {
                        try {
                            File file = new File(subDir, fileName);
                            outputStream.writeObject(new BinaryInfoImpl(fileName, null, file.length()));
                            outputStream.flush();
                        } catch (Exception e) {
                            log.error("Failed to collect binaries list from providers", e);
                        }
                    }
                }
            }
        }
    }

    private class ReactivateBinaryProviderCollector implements BinaryProviderCollector {

        private List<String> inactiveProviders;

        public ReactivateBinaryProviderCollector(List<String> inactiveProviders) {
            this.inactiveProviders = inactiveProviders;
        }

        @Override
        public void collect(BinaryProviderBase binaryProviderBase) {
            if (binaryProviderBase instanceof StateAwareBinaryProvider) {
                StateAwareBinaryProvider stateAware = (StateAwareBinaryProvider) binaryProviderBase;
                boolean active = stateAware.tryToActivate();
                if (!active) {
                    inactiveProviders.add(binaryProviderBase.getProviderMetaData().getId());
                }
            }
        }
    }

    private class OptimizationBinaryProviderVisitor implements BinaryProviderCollector {
        @Override
        public void collect(BinaryProviderBase binaryProviderBase) {
            if (binaryProviderBase instanceof BinaryProviderOptimizationSupport) {
                BinaryProviderOptimizationSupport provider = (BinaryProviderOptimizationSupport) binaryProviderBase;
                provider.optimize();
            }
        }
    }
}
