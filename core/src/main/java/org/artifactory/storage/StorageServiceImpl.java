/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.ArtifactoryDbDataStoreImpl;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.jcr.utils.DerbyUtils;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.mbean.Storage;
import org.artifactory.storage.mbean.StorageMBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author yoavl
 */
@Service
@Reloadable(beanClass = InternalStorageService.class, initAfter = JcrService.class)
public class StorageServiceImpl implements InternalStorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private TaskService taskService;

    private boolean derbyUsed;

    public void compress(MultiStatusHolder statusHolder) {
        if (!derbyUsed) {
            statusHolder.setError("Compress command is not supported on current database type.", log);
            return;
        }

        logStorageSizes();
        DerbyUtils.compress(statusHolder);
        logStorageSizes();
    }

    public void logStorageSizes() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-----Storage sizes-----\n");
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File dataDir = artifactoryHome.getDataDir();
        File[] dirs = {new File(dataDir, "db"), new File(dataDir, "store"), new File(dataDir, "index")};
        for (File dir : dirs) {
            if (dir.exists()) {
                long sizeOfDirectory = FileUtils.sizeOfDirectory(dir);
                String sizeOfDirectoryGb = StorageUnit.toReadableString(sizeOfDirectory);
                sb.append(dir.getName()).append("=").append(sizeOfDirectory).append(" bytes ").append(" (").append(
                        (sizeOfDirectoryGb)).append(")").append("\n");
            }
        }
        long storageSize = getStorageSize();
        String sizeOfDirectoryGb = StorageUnit.toReadableString(storageSize);
        sb.append("datastore table=").append(storageSize).append(" bytes ").append(" (").append(
                (sizeOfDirectoryGb)).append(")").append("\n");
        sb.append("-----------------------");
        log.info(sb.toString());
    }

    public long getStorageSize() {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            ExtendedDbDataStore dataStore = JcrUtils.getDataStore(repository);
            return dataStore.getStorageSize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate storage size.", e);
        } finally {
            session.logout();
        }
    }

    public void callManualGarbageCollect(MultiStatusHolder statusHolder) {
        taskService.checkCanStartManualTask(JcrGarbageCollectorJob.class, statusHolder);
        if (!statusHolder.isError()) {
            try {
                String firstToken = execOneGcAndWait(false);
                InternalStorageService me = InternalContextHelper.get().getBean(InternalStorageService.class);
                me.asyncManualGarbageCollect(firstToken);
                statusHolder.setStatus("Artifactory Storage Garbage Collector process activated in the background!",
                        log);
            } catch (Exception e) {
                statusHolder.setError("Error activating Artifactory Storage Garbage Collector: " + e.getMessage(), e,
                        log);
            }
        }
    }

    public void asyncManualGarbageCollect(String firstRunToken) {
        taskService.waitForTaskCompletion(firstRunToken);
        execOneGcAndWait(true);
    }

    public void manualGarbageCollect() {
        try {
            //GC in-use-records weak references used by the file datastore
            System.gc();
            log.info("Scheduling manual garbage collector to run immediately.");
            execOneGcAndWait(true);
            execOneGcAndWait(true);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in executing the manual garbage collector.", e);
        }
    }

    private String execOneGcAndWait(boolean waitForCompletion) {
        TaskBase task = TaskUtils.createManualTask(JcrGarbageCollectorJob.class, 0L);
        task.addAttribute(JcrGarbageCollectorJob.FIX_CONSISTENCY, "true");
        String token = taskService.startTask(task, true);
        if (waitForCompletion) {
            taskService.waitForTaskCompletion(token);
        }
        return token;
    }

    public boolean isDerbyUsed() {
        return derbyUsed;
    }

    public void init() {
        derbyUsed = DerbyUtils.isDerbyUsed();
        InternalContextHelper.get().registerArtifactoryMBean(new Storage(this), StorageMBean.class, null);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        derbyUsed = DerbyUtils.isDerbyUsed();
    }

    public void destroy() {
        //nop
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //nop
    }

    public void exportDbDataStore(String destDir) {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            ExtendedDbDataStore dataStore = JcrUtils.getDataStore(repository);
            if (dataStore instanceof ArtifactoryDbDataStoreImpl) {
                try {
                    ((ArtifactoryDbDataStoreImpl) dataStore).exportData(destDir);
                } catch (Exception e) {
                    log.error("Failed to export datasource data.", e);
                }
            } else {
                throw new IllegalArgumentException("Datasource used is not a db one.");
            }
        } finally {
            session.logout();
        }
    }
}