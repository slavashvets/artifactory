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

package org.artifactory.storage.binstore.binary.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.annotation.Nonnull;

import org.artifactory.storage.binstore.binary.providers.base.BinaryProviderBase;
import org.artifactory.storage.binstore.binary.providers.base.StorageInfo;
import org.artifactory.storage.binstore.binary.providers.base.StorageInfoImpl;
import org.artifactory.storage.binstore.binary.providers.tools.FilePersistenceHelper;
import org.artifactory.storage.binstore.exceptions.BinaryNotFoundException;
import org.artifactory.storage.binstore.exceptions.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 12/16/12
 * Time: 10:44 AM
 *
 * @author freds
 */
public abstract class FileBinaryProviderReadOnlyBase extends BinaryProviderBase
        implements FileBinaryProvider {
    private static final Logger log = LoggerFactory.getLogger(FileBinaryProviderReadOnlyBase.class);

    protected File binariesDir;
    protected File tempBinariesDir;
    protected long freeSpace;

    @Override
    public void initialize() {
        // Main filestore directory
        this.binariesDir = new File(getParam("binariesDir"));
        // insert the final directory into the metadata for UI usage (UI doesn't have access to to the binary providers)
        this.tempBinariesDir = new File(binariesDir, getParam("tempDir"));
        verifyState();
    }

    protected String getFileStoreDir() {
        return getParam("fileStoreDir");
    }

    @Override
    public boolean isAccessible() {
        try {
            if (this.binariesDir == null || this.tempBinariesDir == null) {
                return false;
            }
            verifyState();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void verifyState() {
        if (!binariesDir.exists() && !binariesDir.mkdirs()) {
            throw new StorageException("Could not create file store folder '" + binariesDir.getAbsolutePath() + "'");
        }
        if (!tempBinariesDir.exists() && !tempBinariesDir.mkdirs()) {
            throw new StorageException("Could not create temporary pre store folder '" +
                    tempBinariesDir.getAbsolutePath() + "'");
        }
        freeSpace = getBinariesDir().getFreeSpace();
    }

    @Override
    public long getFreeSpace() {
        return freeSpace;
    }

    @Nonnull
    @Override
    public File getBinariesDir() {
        return binariesDir;
    }

    @Override
    public boolean exists(String sha1) {
        File file = getFile(sha1);
        if (file.exists()) {
            return true;
        } else {
            log.trace("File not found: {}", file.getAbsolutePath());
        }
        return next().exists(sha1);
    }

    @Override
    @Nonnull
    public File getFile(String sha1) {
        return new File(binariesDir, FilePersistenceHelper.getRelativePath(sha1));
    }

    @Override
    public boolean isFileExists(String sha1) {
        return exists(sha1);
    }

    @Nonnull
    @Override
    public InputStream getStream(String sha1) {
        File file = getFile(sha1);
        try {
            if (!file.exists()) {
                log.trace("File not found: {}", file.getAbsolutePath());
                return next().getStream(sha1);
            }
            log.trace("File found: {}", file.getAbsolutePath());
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new BinaryNotFoundException("Couldn't access file '" + file.getAbsolutePath() + "'", e);
        }
    }

    @Nonnull
    @Override
    public File createTempFile() {
        return FilePersistenceHelper.createTempBinFile(tempBinariesDir);
    }

    @Nonnull
    @Override
    public StorageInfo getStorageInfo() {
        long freeSpace = getBinariesDir().getFreeSpace();
        long totalSpace = getBinariesDir().getTotalSpace();
        long usageSpace = totalSpace - freeSpace;
        long usageSpaceInPercent = (long) (((double) usageSpace) / ((double) totalSpace) * 100d);
        long freeSpaceInPercent = 100 - usageSpaceInPercent;
        return new StorageInfoImpl(freeSpace, totalSpace, usageSpace, usageSpaceInPercent, freeSpaceInPercent);
    }
}
