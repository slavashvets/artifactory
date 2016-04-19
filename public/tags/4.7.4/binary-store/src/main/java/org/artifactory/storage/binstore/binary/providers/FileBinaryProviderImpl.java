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

import org.artifactory.storage.binstore.annotation.BinaryProviderClassInfo;
import org.artifactory.storage.binstore.binary.providers.base.BinaryInfo;
import org.artifactory.storage.binstore.binary.providers.base.BinaryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.artifactory.storage.binstore.binary.providers.tools.FilePersistenceHelper.saveStreamFileAndMove;

/**
 * A binary provider that manage low level checksum files on filesystem.
 *
 * @author Fred Simon
 */
@BinaryProviderClassInfo(nativeName = "file-system")
public class FileBinaryProviderImpl extends FileBinaryProviderBase {
    private static final Logger log = LoggerFactory.getLogger(FileBinaryProviderImpl.class);

    @Override
    @Nonnull
    public BinaryInfo addStream(BinaryStream in) throws IOException {
        return saveStreamFileAndMove(in, this);
    }
}
