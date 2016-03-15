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

import org.artifactory.storage.binstore.exceptions.StorageException;
import org.artifactory.storage.binstore.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Date: 12/13/12
 * Time: 7:56 AM
 *
 * @author freds
 */
public abstract class FileBinaryProviderBase extends FileBinaryProviderReadOnlyBase {
    private static final Logger log = LoggerFactory.getLogger(FileBinaryProviderBase.class);

    public static boolean removeFile(File file) {
        if (file != null && file.exists()) {
            //Try to delete the file
            if (!FileUtils.removeFile(file)) {
                log.warn("Unable to remove " + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    @Override
    protected void verifyState() {
        super.verifyState();
        // the main and pre folder should be writable also
        if (!binariesDir.canWrite()) {
            throw new StorageException("Filestore folder '" +
                    binariesDir.getAbsolutePath() + "' is not writable!");
        }
        if (!tempBinariesDir.canWrite()) {
            throw new StorageException("Temporary pre store folder '" +
                    tempBinariesDir.getAbsolutePath() + "' is not writable!");
        }
    }

    @Override
    public boolean delete(String sha1) {
        if (deleteNoChain(sha1)) {
            return next().delete(sha1);
        }
        return false;
    }

    protected boolean deleteNoChain(String sha1) {
        File file = getFile(sha1);
        log.debug("Deleting file {}", file.getAbsolutePath());
        removeFile(file);
        if (file.exists()) {
            log.error("Could not delete file " + file.getAbsolutePath());
            return false;
        } else {
            return true;
        }
    }
}
