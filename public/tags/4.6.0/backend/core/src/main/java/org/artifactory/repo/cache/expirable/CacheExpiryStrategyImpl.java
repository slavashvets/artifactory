package org.artifactory.repo.cache.expirable;

import org.artifactory.fs.RepoResource;
import org.artifactory.request.RepoRequests;

/**
 * Default implementation of expiry strategy which checks the file last modified date
 *
 * @author Shay Yaakov
 */
public class CacheExpiryStrategyImpl implements CacheExpiryStrategy {

    @Override
    public boolean foundExpiredAndRemoteIsNewer(RepoResource remoteResource, RepoResource cachedResource) {
        boolean remoteIsNewer = cachedResource.isExpired() && remoteResource.getLastModified() > cachedResource.getLastModified();
        RepoRequests.logToContext("Found expired cached resource but remote is newer = %s. Cached resource: %s, Remote resource: %s",
                remoteIsNewer, cachedResource.getLastModified(), remoteResource.getLastModified());
        return remoteIsNewer;
    }
}
