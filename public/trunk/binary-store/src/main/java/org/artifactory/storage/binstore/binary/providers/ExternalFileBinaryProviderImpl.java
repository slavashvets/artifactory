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

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A binary provider that provide streams from an external filestore.
 * Will throw exception on all insert and prune methods.
 *
 * @author Fred Simon
 */

@BinaryProviderClassInfo(nativeName = "external-file")
public class ExternalFileBinaryProviderImpl extends FileBinaryProviderReadOnlyBase {

    @Override
    @Nonnull
    public BinaryInfo addStream(BinaryStream in) throws IOException {
        // No add to an external binary provider
        return next().addStream(in);
    }

    @Override
    public boolean delete(String sha1) {
        // No deletion of an external binary provider
        return next().delete(sha1);
    }
}
