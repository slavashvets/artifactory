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

package org.artifactory.repo;

import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.md.Properties;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.ResourceStreamHandle;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;

public interface RemoteRepo<T extends RemoteRepoDescriptor> extends RealRepo<T> {
    long getRetrievalCachePeriodSecs();

    boolean isStoreArtifactsLocally();

    boolean isHardFail();

    String getUrl();

    LocalCacheRepo getLocalCacheRepo();

    /**
     * Downloads a resource from the remote repository
     *
     * @return A handle for the remote resource
     */
    ResourceStreamHandle downloadResource(String relPath, Properties requestProperties) throws IOException;

    /**
     * Retrieves a resource remotely if the remote resource was found and is newer
     */
    ResourceStreamHandle conditionalRetrieveResource(String relPath) throws IOException;

    long getFailedRetrievalCachePeriodSecs();

    long getMissedRetrievalCachePeriodSecs();

    void clearCaches();

    /**
     * Removes a path from the repository caches (missed and failed)
     *
     * @param path           The path to remove from the cache. The path is relative path from the repository root.
     * @param removeSubPaths If true will also remove any sub paths from the caches.
     */
    void removeFromCaches(String path, boolean removeSubPaths);

    boolean isOffline();

    /**
     * Performs the actual remote download of the artifact.
     *
     * @param requestContext
     * @param remoteResource A remote resource that has been returned by getInfo()
     * @param cachedResource
     * @return
     * @throws IOException
     * @throws RepositoryException
     * @throws org.artifactory.api.repo.exception.RepoRejectException
     *
     */
    ResourceStreamHandle downloadAndSave(RequestContext requestContext, RepoResource remoteResource,
            RepoResource cachedResource) throws IOException, RepositoryException, RepoRejectException;

    /**
     * List remote resources from a remote path.
     *
     * @param directoryRepoPath The path of the remote repository listing
     * @return A list of URLs that represent the remote hrefs of the remote resources.
     * @throws IOException On any communication of parsing exception
     */
    @Nonnull
    List<String> listRemoteResources(RepoPath directoryRepoPath) throws IOException;

    /**
     * @return True if this repo supports listing remote directories AND it's not offline AND it's not blacklisted.
     */
    boolean isListRemoteFolderItems();
}