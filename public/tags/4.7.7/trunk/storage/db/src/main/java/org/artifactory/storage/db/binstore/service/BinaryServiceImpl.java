/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.storage.db.binstore.service;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.BinariesInfo;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.storage.BinaryInsertRetryException;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.BinaryInfo;
import org.artifactory.storage.binstore.service.GarbageCollectorInfo;
import org.artifactory.storage.binstore.service.GarbageCollectorListener;
import org.artifactory.storage.binstore.service.InternalBinaryService;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.binstore.dao.BinariesDao;
import org.artifactory.storage.db.binstore.entity.BinaryEntity;
import org.artifactory.storage.db.binstore.exceptions.PruneException;
import org.artifactory.storage.db.binstore.visitors.BinaryTreeElementScanner;
import org.artifactory.storage.db.binstore.visitors.EssentialBinaryTreeElementHandler;
import org.artifactory.storage.fs.service.ArchiveEntriesService;
import org.artifactory.storage.model.FileBinaryProviderInfo;
import org.artifactory.util.Pair;
import org.jfrog.storage.binstore.common.BinaryProviderManagerImpl;
import org.jfrog.storage.binstore.common.ReaderTrackingInputStream;
import org.jfrog.storage.binstore.ifc.*;
import org.jfrog.storage.binstore.ifc.model.BinaryElement;
import org.jfrog.storage.binstore.ifc.model.BinaryTreeElement;
import org.jfrog.storage.binstore.ifc.model.StorageInfo;
import org.jfrog.storage.binstore.ifc.provider.BinaryProvider;
import org.jfrog.storage.binstore.providers.base.StorageInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * The main binary store of Artifactory that delegates to the BinaryProvider chain.
 *
 * @author Yossi Shaul
 */
@Service
public class BinaryServiceImpl implements InternalBinaryService {
    private static final Logger log = LoggerFactory.getLogger(BinaryServiceImpl.class);

    @Autowired
    private BinariesDao binariesDao;

    @Autowired
    private ArchiveEntriesService archiveEntriesService;

    @Autowired
    private DbService dbService;

    @Autowired
    private StorageProperties storageProperties;

    /**
     * Map of delete protected sha1 checksums to the number of protections (active readers + writer count for each binary)
     */
    private ConcurrentMap<String, Pair<AtomicInteger, Long>> deleteProtectedBinaries;
    private List<GarbageCollectorListener> garbageCollectorListeners;
    private BinaryProviderManager binaryProviderManager;
    private BinaryProviderConfig defaultValues;
    private Lock lock = new ReentrantLock();
    private BinaryProvider binaryProvider;
    private boolean forceBinaryProviderOptimizationOnce = false;

    @PostConstruct
    public void initialize() {
        garbageCollectorListeners = new CopyOnWriteArrayList<>();
        log.debug("Initializing the ConfigurableBinaryProviderManager");
        deleteProtectedBinaries = new MapMaker().makeMap();
        // Generate Default values
        defaultValues = storageProperties.toDefaultValues();
        // Set the binarystore.xml file location
        File haAwareEtcDir = ArtifactoryHome.get().getHaAwareEtcDir();
        File userConfigFile = new File(haAwareEtcDir, "binarystore.xml");
        defaultValues.setBinaryStoreXmlPath(userConfigFile.getAbsolutePath());
        // Finally create an instancethe binary provider manager
        binaryProviderManager = new BinaryProviderManagerImpl(defaultValues);
        // Get the root binary provide from the binary provider manager
        binaryProvider = binaryProviderManager.getFirstBinaryProvider();
    }

    @Override
    public BinaryProviderManager getBinaryProviderManager() {
        return binaryProviderManager;
    }

    @PreDestroy
    public void destroy() {
        notifyGCListenersOnDestroy();
    }

    @Override
    public void addGCListener(GarbageCollectorListener garbageCollectorListener) {
        garbageCollectorListeners.add(garbageCollectorListener);
    }

    @Override
    public void addExternalFileStore(File externalFileDir, ProviderConnectMode connectMode) {
        // The external binary provider works only if the file binary provider is not null
        if (getBinariesDir() == null) {
            return;
        }
        // Prepare parameters for the new External binary provider
        String mode = connectMode.propName;
        String externalDir = externalFileDir.getAbsolutePath();
        String fileStoreDir = defaultValues.getParam("fileStoreDir");
        File fileStoreFullPath = new File(new File(defaultValues.getParam("baseDataDir")), fileStoreDir);
        // create and initialize the external binary providers.
        binaryProviderManager.initializeExternalBinaryProvider(mode, externalDir, fileStoreFullPath.getAbsolutePath(), defaultValues);
    }

    @Override
    public void disconnectExternalFilestore(File externalDir, ProviderConnectMode disconnectMode,
                                            BasicStatusHolder statusHolder) {
        ExternalBinaryProviderHelper.disconnectFromFileStore(this, externalDir, disconnectMode, statusHolder, binaryProviderManager, binariesDao, defaultValues);
    }

    @Override
    public File getBinariesDir() {
        // Get binary providers info tree from the manager
        BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo = binaryProviderManager.getBinaryProvidersInfo();
        // Collect all the file binary providers in list
        List<FileBinaryProviderInfo> providersInfos = Lists.newArrayList();
        collectFileBinaryProvidersDirsInternal(providersInfos, binaryProvidersInfo);
        // Get the First binary provider
        FileBinaryProviderInfo fileBinaryProviderInfo = providersInfos.size() > 0 ? providersInfos.get(0) : null;
        if (fileBinaryProviderInfo != null) {
            // We need the wrapper to avoid binary dir recalculation even if there is no file binary provider
            return fileBinaryProviderInfo.getFileStoreDir();
        }
        return null;
    }

    @Override
    public StorageInfo getStorageInfoSummary() {
        // Get binary providers info tree from the manager
        BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo = binaryProviderManager.getBinaryProvidersInfo();
        // Collect all the  binary providers in list
        List<BinaryTreeElement<BinaryProviderInfo>> providersInfos = Lists.newArrayList();
        collectBinaryProviderInfo(providersInfos, binaryProvidersInfo);
        // Remove the cache binary provider since it doesn't includes its next binary provider storage summary
        Iterator<BinaryTreeElement<BinaryProviderInfo>> iterator = providersInfos.iterator();
        while (iterator.hasNext()) {
            BinaryTreeElement<BinaryProviderInfo> next = iterator.next();
            if (next != null && next.getData() != null) {
                String type = next.getData().getProperties().get("type");
                if ("cache-fs".equals(type)) {
                    iterator.remove();
                }
            }
        }
        if (providersInfos.size() > 0) {
            return providersInfos.get(0).getData().getStorageInfo();
        }
        return new StorageInfoImpl(-1, -1, -1, -1, -1);
    }

    @Override
    @Nullable
    public BinaryInfo addBinaryRecord(String sha1, String md5, long length) {
        try {
            BinaryEntity result = binariesDao.load(sha1);
            if (result == null) {
                // It does not exists in the DB
                // Let's check if in bin provider
                if (binaryProvider.exists(sha1)) {
                    // Good let's use it
                    return getTransactionalMe().insertRecordInDb(sha1, md5, length);
                }
                return null;
            }
            return convertToBinaryInfo(result);
        } catch (SQLException e) {
            throw new StorageException("Could not reserved entry '" + sha1 + "'", e);
        }
    }

    @Override
    @Nonnull
    public BinaryInfo addBinary(InputStream in) throws IOException {
        if (in instanceof BinaryServiceInputStream) {
            throw new IllegalStateException("Cannot add binary from checksum deploy "
                    + ((BinaryServiceInputStream) in).getBinaryInfo());
        }

        BinaryInfo binaryInfo;
        BinaryElement bi = binaryProvider.addStream(binaryProviderManager.createBinaryStream(in));
        log.trace("Inserted binary {} to file store", bi.getSha1());
        // From here we managed to create a binary record on the binary provider
        // So, failing on the insert in DB (because saving the file took to long)
        // can be re-tried based on the sha1
        try {
            binaryInfo = getTransactionalMe().insertRecordInDb(bi.getSha1(), bi.getMd5(), bi.getLength());
        } catch (BinaryInsertRetryException e) {
            if (log.isDebugEnabled()) {
                log.info("Retrying add binary after receiving exception", e);
            } else {
                log.info("Retrying add binary after receiving exception: " + e.getMessage());
            }
            binaryInfo = addBinaryRecord(bi.getSha1(), bi.getMd5(), bi.getLength());
            if (binaryInfo == null) {
                throw new StorageException("Failed to add binary record with SHA1 " + bi.getSha1() +
                        "during retry", e);
            }
        }
        return binaryInfo;
    }

    @Override
    public BinaryTreeElement<Map<String, String>> getBinaryProvidersInfo() {
        // Get binary providers info tree from the binary store manager
        BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo = binaryProviderManager.getBinaryProvidersInfo();
        // Create sub tree that contains only essential elements (for the UI)
        BinaryTreeElementScanner<BinaryProviderInfo, Map<String, String>> scanner = new BinaryTreeElementScanner<>();
        EssentialBinaryTreeElementHandler handler = new EssentialBinaryTreeElementHandler();
        return scanner.scan(binaryProvidersInfo, handler);
    }

    @Override
    public InputStream getBinary(String sha1) {
        return new ReaderTrackingInputStream(binaryProvider.getStream(sha1), sha1, this);
    }

    @Override
    public InputStream getBinary(BinaryInfo bi) {
        if (!binaryProvider.exists(bi.getSha1())) {
            return null;
        }
        return new BinaryServiceInputStreamWrapper(bi, this);
    }

    class BinaryServiceInputStreamWrapper extends ReaderTrackingInputStream implements BinaryServiceInputStream {
        private BinaryInfo bi;

        public BinaryServiceInputStreamWrapper(BinaryInfo bi, UsageTracking usageTracking) {
            super(null, bi.getSha1(), usageTracking);
            this.bi = bi;
        }

        @Nonnull
        @Override
        public BinaryInfo getBinaryInfo() {
            return bi;
        }
    }

    @Override
    public BinaryInfo findBinary(String sha1) {
        try {
            BinaryEntity result = binariesDao.load(sha1);
            if (result != null) {
                return convertToBinaryInfo(result);
            }
        } catch (SQLException e) {
            throw new StorageException("Storage error loading checksum '" + sha1 + "'", e);
        }
        return null;
    }

    @Nonnull
    @Override
    public Set<BinaryInfo> findBinaries(@Nullable Collection<String> checksums) {
        Set<BinaryInfo> results = Sets.newHashSet();
        if (checksums == null || checksums.isEmpty()) {
            return results;
        }
        try {
            for (ChecksumType checksumType : ChecksumType.BASE_CHECKSUM_TYPES) {
                Collection<String> validChecksums = extractValid(checksumType, checksums);
                if (!validChecksums.isEmpty()) {
                    Collection<BinaryEntity> found = binariesDao.search(checksumType, validChecksums);
                    results.addAll(found.stream().map(this::convertToBinaryInfo).collect(Collectors.toList()));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Could not search for checksums " + checksums, e);
        }
        return results;
    }

    @Override
    public GarbageCollectorInfo garbageCollect() {
        notifyGCListenersOnStart();
        final GarbageCollectorInfo result = new GarbageCollectorInfo();
        Collection<BinaryEntity> binsToDelete;
        try {
            BinariesInfo countAndSize = binariesDao.getCountAndTotalSize();
            result.initialCount = countAndSize.getBinariesCount();
            result.initialSize = countAndSize.getBinariesSize();
            binsToDelete = binariesDao.findPotentialDeletion();
        } catch (SQLException e) {
            throw new StorageException("Could not find potential Binaries to delete!", e);
        }
        result.stopScanTimestamp = System.currentTimeMillis();
        result.candidatesForDeletion = binsToDelete.size();
        if (result.candidatesForDeletion > 0) {
            log.info("Found {} candidates for deletion", result.candidatesForDeletion);
        }
        for (BinaryEntity bd : binsToDelete) {
            log.trace("Candidate for deletion: {}", bd);
            dbService.invokeInTransaction("BinaryCleaner#" + bd.getSha1(), new BinaryCleaner(bd, result));
        }

        if (result.checksumsCleaned > 0) {
            result.archivePathsCleaned = getTransactionalMe().deleteUnusedArchivePaths();
            result.archiveNamesCleaned = getTransactionalMe().deleteUnusedArchiveNames();
        }

        result.gcEndTime = System.currentTimeMillis();

        try {
            BinariesInfo countAndSize = binariesDao.getCountAndTotalSize();
            result.printCollectionInfo(countAndSize.getBinariesSize());
        } catch (SQLException e) {
            log.error("Could not list files due to " + e.getMessage());
        }
        boolean success = binaryProviderManager.optimize(forceBinaryProviderOptimizationOnce);
        if (success) {
            forceBinaryProviderOptimizationOnce = false;
        }
        notifyGCListenersOnFinished();
        return result;
    }

    /**
     * Deletes binary row and all dependent rows from the database
     *
     * @param sha1ToDelete Checksum to delete
     * @return True if deleted. False if not found or error
     */
    private boolean deleteEntry(String sha1ToDelete) {
        boolean hadArchiveEntries;
        try {
            hadArchiveEntries = archiveEntriesService.deleteArchiveEntries(sha1ToDelete);
        } catch (Exception e) {
            log.error("Failed to delete archive entries for " + sha1ToDelete, e);
            return false;
        }
        try {
            boolean entryDeleted = binariesDao.deleteEntry(sha1ToDelete) == 1;
            if (!entryDeleted && hadArchiveEntries) {
                log.error("Binary entry " + sha1ToDelete + " had archive entries that are deleted," +
                        " but the binary line was not deleted! Re indexing of archive needed.");
            }
            return entryDeleted;
        } catch (SQLException e) {
            log.error("Could execute delete from binary store of " + sha1ToDelete, e);
        }
        return false;
    }

    @Override
    public int deleteUnusedArchivePaths() {
        try {
            log.debug("Deleting unused archive paths");
            return archiveEntriesService.deleteUnusedPathIds();
        } catch (StorageException e) {
            log.error("Failed to delete unique paths: {}", e.getMessage());
            log.debug("Failed to delete unique paths", e);
            return 0;
        }
    }

    @Override
    public int deleteUnusedArchiveNames() {
        try {
            log.debug("Deleting unused archive names");
            return archiveEntriesService.deleteUnusedNameIds();
        } catch (StorageException e) {
            log.error("Failed to delete unique archive names: {}", e.getMessage());
            log.debug("Failed to delete unique archive paths", e);
            return 0;
        }
    }

    @Override
    public int incrementNoDeleteLock(String sha1) {
        Pair<AtomicInteger, Long> pair = deleteProtectedBinaries.putIfAbsent(sha1, new Pair<>(new AtomicInteger(1), System.currentTimeMillis()));
        if (pair == null) {
            return 1;
        } else {
            pair.setSecond(System.currentTimeMillis());
            return pair.getFirst().incrementAndGet();
        }
    }

    @Override
    public void decrementNoDeleteLock(String sha1) {
        AtomicInteger usageCount = deleteProtectedBinaries.get(sha1).getFirst();
        if (usageCount != null) {
            usageCount.decrementAndGet();
        }
    }

    @Override
    public Collection<BinaryInfo> findAllBinaries() {
        try {
            Collection<BinaryEntity> allBinaries = binariesDao.findAll();
            List<BinaryInfo> result = new ArrayList<>(allBinaries.size());
            result.addAll(allBinaries.stream().map(this::convertToBinaryInfo).collect(Collectors.toList()));
            return result;
        } catch (SQLException e) {
            throw new StorageException("Could not retrieve all binary entries", e);
        }
    }

    @Override
    @Nonnull
    public BinaryInfo insertRecordInDb(String sha1, String md5, long length) throws StorageException {
        BinaryEntityWithValidation dataRecord = new BinaryEntityWithValidation(sha1, md5, length);
        if (!dataRecord.isValid()) {
            throw new StorageException("Cannot insert invalid binary record: " + dataRecord);
        }
        try {
            boolean binaryExists = binariesDao.exists(sha1);
            if (!binaryExists) {
                createDataRecord(dataRecord, sha1);
            }
            // Always reselect from DB before returning
            BinaryEntity justInserted = binariesDao.load(sha1);
            if (justInserted == null) {
                throw new StorageException("Could not find just inserted binary record: " + dataRecord);
            }
            return convertToBinaryInfo(justInserted);
        } catch (SQLException e) {
            throw new StorageException("Failed to insert new binary record: " + e.getMessage(), e);
        }
    }

    /**
     * @return Number of binaries and total size stored in the binary store
     */
    @Override
    public BinariesInfo getBinariesInfo() {
        try {
            return binariesDao.getCountAndTotalSize();
        } catch (SQLException e) {
            throw new StorageException("Could not calculate total size due to " + e.getMessage(), e);
        }
    }

    @Override
    public long getStorageSize() {
        return getBinariesInfo().getBinariesSize();
    }

    @Override
    public void ping() {
        // Ping to storage
        binaryProviderManager.ping();
        // Ping to DB
        try {
            if (binariesDao.exists("does not exists")) {
                throw new StorageException("Select entry fails");
            }
        } catch (SQLException e) {
            throw new StorageException("Accessing Binary Store DB failed with " + e.getMessage(), e);
        }
    }

    @Override
    public void prune(BasicStatusHolder statusHolder) {
        boolean locked = lock.tryLock();
        if (locked) {
            try {
                pruneInternal(statusHolder);
            } finally {
                lock.unlock();
            }
        } else {
            throw new PruneException("The prune process is already running");
        }
    }

    /**
     * @param sha1 sha1 checksum of the binary to check
     * @return True if the given binary is currently used by a reader (e.g., open stream) or writer
     */
    @Override
    public boolean isActivelyUsed(String sha1) {
        Pair<AtomicInteger, Long> pair = deleteProtectedBinaries.get(sha1);
        return pair != null && pair.getFirst().get() > 0;
    }

    private Collection<String> extractValid(ChecksumType checksumType, Collection<String> checksums) {
        Collection<String> results = Sets.newHashSet();
        results.addAll(checksums.stream().filter(checksumType::isValid).collect(Collectors.toList()));
        return results;
    }

    private InternalBinaryService getTransactionalMe() {
        return ContextHelper.get().beanForType(InternalBinaryService.class);
    }

    private BinaryInfo convertToBinaryInfo(BinaryEntity bd) {
        return new BinaryInfoImpl(bd.getSha1(), bd.getMd5(), bd.getLength());
    }

    private void notifyGCListenersOnStart() {
        garbageCollectorListeners.forEach(GarbageCollectorListener::start);
    }

    private void notifyGCListenersOnFinished() {
        garbageCollectorListeners.forEach(GarbageCollectorListener::finished);
    }

    private void notifyGCListenersOnDestroy() {
        garbageCollectorListeners.forEach(GarbageCollectorListener::destroy);
    }

    private Map<String, BinaryEntity> isInStore(Set<String> sha1List) {
        try {
            return binariesDao.search(ChecksumType.sha1, sha1List).stream().collect(
                    Collectors.toMap(BinaryEntity::getSha1, (p) -> p));
        } catch (SQLException e) {
            throw new StorageException("Could search for checksum list!", e);
        }
    }

    @Override
    public List<String> getAndManageErrors() {
        List<String> errors = binaryProviderManager.getErrors();
        if (errors.size() > 0) {
            forceOptimizationOnce();
        }
        return errors;
    }

    @Override
    public void forceOptimizationOnce() {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        if (haCommonAddon.isHaEnabled() && !haCommonAddon.isPrimary()) {
            haCommonAddon.forceOptimizationOnce();
        } else {
            forceBinaryProviderOptimizationOnce = true;
        }
    }

    private void createDataRecord(BinaryEntity dataRecord, String sha1) throws SQLException {
        // insert a new binary record to the db
        try {
            binariesDao.create(dataRecord);
        } catch (SQLException e) {
            if (isDuplicatedEntryException(e)) {
                log.debug("Simultaneous insert of binary {} detected, binary will be checked.", sha1, e);
                throw new BinaryInsertRetryException(convertToBinaryInfo(dataRecord), e);
            } else {
                throw e;
            }
        }
    }

    private boolean isDuplicatedEntryException(SQLException exception) {
        String message = exception.getMessage();
        return message.contains("duplicate key") // Derby message
                || message.contains("Duplicate entry") // MySQL message
                || message.contains("unique constraint"); // Oracle message
    }

    private void pruneInternal(BasicStatusHolder statusHolder) {
        long start = System.currentTimeMillis();
        long filesMoved = 0;
        long totalSize = 0;
        int chunkSize = ConstantValues.binaryProviderPruneChunkSize.getInt();
        try (ObjectInputStream stream = binaryProviderManager.getBinariesDataStream()) {
            HashSet<BinaryElement> chunk = Sets.newHashSet();
            while (true) {
                BinaryElement binaryInfo = (BinaryElement) stream.readObject();
                if (binaryInfo != null) {
                    chunk.add(binaryInfo);
                }
                if (chunk.size() >= chunkSize || binaryInfo == null) {
                    Map<String, BinaryEntity> inStore = isInStore(chunk.stream().map(BinaryElement::getSha1).collect(Collectors.toSet()));
                    for (BinaryElement data : chunk) {
                        if (!inStore.keySet().contains(data.getSha1())) {
                            if (isActivelyUsed(data.getSha1())) {
                                statusHolder.status("Skipping deletion for in-use artifact record: " + data.getSha1(), log);
                            } else {
                                boolean delete = binaryProvider.delete(data.getSha1());
                                if (!delete) {
                                    statusHolder.error("Could not delete file " + data.getSha1(), log);
                                } else {
                                    filesMoved++;
                                    totalSize += data.getLength();
                                }
                            }
                        }
                    }
                    if (binaryInfo == null) {
                        break;
                    } else {
                        chunk.clear();
                    }
                }
            }
        } catch (Exception e) {
            statusHolder.error("Failed to execute prune, cause: " + e.getMessage(), e, log);
            log.error("Failed to execute prune, cause: ", e);
        }

        long tt = (System.currentTimeMillis() - start);
        statusHolder.status("Removed " + filesMoved
                + " files in total size of " + StorageUnit.toReadableString(totalSize)
                + " (" + tt + "ms).", log);
    }

    public boolean isFileExist(String sha1) {
        return binaryProvider.exists(sha1);
    }

    /**
     * Deletes a single binary from the database and filesystem if not in use.
     */
    private class BinaryCleaner implements Callable<Void> {
        private final GarbageCollectorInfo result;
        private final BinaryEntity bd;

        public BinaryCleaner(BinaryEntity bd, GarbageCollectorInfo result) {
            this.result = result;
            this.bd = bd;
        }

        @Override
        public Void call() throws Exception {
            String sha1 = bd.getSha1();
            deleteProtectedBinaries.putIfAbsent(sha1, new Pair<>(new AtomicInteger(0), System.currentTimeMillis()));
            Pair<AtomicInteger, Long> pair = deleteProtectedBinaries.get(sha1);
            if (pair.getFirst().compareAndSet(0, -30)) {
                log.debug("Targeting '{}' for deletion as it not seems to be used", sha1);
                try {
                    if (deleteEntry(sha1)) {
                        log.trace("Deleted {} record from binaries table", sha1);
                        result.checksumsCleaned++;
                        if (binaryProvider.delete(sha1)) {
                            log.trace("Deleted {} binary", sha1);
                            result.binariesCleaned++;
                            result.totalSizeCleaned += bd.getLength();
                        } else {
                            log.error("Could not delete binary '{}'", sha1);
                        }
                    } else {
                        log.debug("Deleting '{}' has failed", sha1);
                    }
                } finally {
                    // remove delete protection (even if delete was not successful)
                    deleteProtectedBinaries.remove(sha1);
                    log.debug("Cleaning '{}' from ref. counter", sha1);
                }
            } else {
                Long timestamp = pair.getSecond();
                log.info("Binary {} has {} readers with last timestamp of {}", sha1, pair.getFirst().get(), timestamp);
                long treshTime = (System.currentTimeMillis() - timestamp) / 1000;
                if (treshTime > ConstantValues.gcReadersMaxTimeSecs.getLong()) {
                    log.info("Binary {} has reached it's max read time, removing it from ref. counter", sha1);
                    deleteProtectedBinaries.remove(sha1);
                } else {
                    log.info("Binary {} is being read! Not deleting.", sha1);
                }
            }
            return null;
        }
    }

    /**
     * Collects a list of file binaries providers
     */
    private static void collectBinaryProviderInfo(List<BinaryTreeElement<BinaryProviderInfo>> list,
                                                  BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo) {
        if (binaryProvidersInfo == null) {
            return;
        }
        list.add(binaryProvidersInfo);
        collectBinaryProviderInfo(list, binaryProvidersInfo.getNextBinaryTreeElement());
        for (BinaryTreeElement<BinaryProviderInfo> elements : binaryProvidersInfo.getSubBinaryTreeElements()) {
            collectBinaryProviderInfo(list, elements);
        }
    }

    /**
     * Collects a list of file binaries providers
     */
    private static void collectFileBinaryProvidersDirsInternal(List<FileBinaryProviderInfo> list,
                                                               BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo) {
        if (binaryProvidersInfo == null) {
            return;
        }
        String type = binaryProvidersInfo.getData().getProperties().get("type");
        if ("file-system".equals(type)) {
            FileBinaryProviderInfo info = createFileBinaryProviderInfo(binaryProvidersInfo, type);
            list.add(info);
        }
        if ("cache-fs".equals(type)) {
            FileBinaryProviderInfo info = createCacheFileBinaryProviderInfo(binaryProvidersInfo, type);
            list.add(info);
        }
        collectFileBinaryProvidersDirsInternal(list, binaryProvidersInfo.getNextBinaryTreeElement());
        for (BinaryTreeElement<BinaryProviderInfo> elements : binaryProvidersInfo.getSubBinaryTreeElements()) {
            collectFileBinaryProvidersDirsInternal(list, elements);
        }
    }

    private static FileBinaryProviderInfo createFileBinaryProviderInfo(BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo, String type) {

        String temp = binaryProvidersInfo.getData().getProperties().get("tempDir");
        String binariesDir = binaryProvidersInfo.getData().getProperties().get("binariesDir");
        if (new File(binariesDir).isAbsolute()) {
            File tempDir = new File(new File(binariesDir), temp);
            File fileStoreDir = new File(binariesDir);
            return new FileBinaryProviderInfo(tempDir, fileStoreDir, type);
        } else {
            File tempDir = new File(new File(binariesDir), temp);
            File fileStoreDir = new File(binariesDir);
            return new FileBinaryProviderInfo(tempDir, fileStoreDir, type);
        }

    }

    private static FileBinaryProviderInfo createCacheFileBinaryProviderInfo(BinaryTreeElement<BinaryProviderInfo> binaryProvidersInfo, String type) {
        String temp = binaryProvidersInfo.getData().getProperties().get("tempDir");
        String binariesDir = binaryProvidersInfo.getData().getProperties().get("binariesDir");
        File tempDir = new File(new File(binariesDir), temp);
        File fileStoreDir = new File(binariesDir);
        return new FileBinaryProviderInfo(tempDir, fileStoreDir, type);
    }

}

