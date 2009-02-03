package org.artifactory.repo;

import org.apache.log4j.Logger;
import org.artifactory.cache.DefaultRetrievalCache;
import org.artifactory.cache.RetrievalCache;
import org.artifactory.engine.RepoAccessException;
import org.artifactory.engine.ResourceStreamHandle;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XmlType(name = "RemoteRepoBaseType", propOrder = {"url", "hardFail", "storeArtifactsLocally",
        "retrievalCachePeriodSecs", "failedRetrievalCachePeriodSecs",
        "missedRetrievalCachePeriodSecs"})
public abstract class RemoteRepoBase extends RepoBase implements RemoteRepo {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RemoteRepoBase.class);

    private String url;
    private boolean hardFail;
    private long retrievalCachePeriodSecs = 43200;//12hrs
    private long failedRetrievalCachePeriodSecs = 30;//30secs
    private long missedRetrievalCachePeriodSecs = 43200;//12hrs
    protected boolean storeArtifactsLocally = true;

    private LocalCacheRepo localCacheRepo;

    private RetrievalCache failedRetrievalsCache;
    private RetrievalCache missedRetrievalsCache;


    public void init(CentralConfig cc) {
        //Initialize the local cache
        if (!isStoreArtifactsLocally()) {
            return;
        }
        localCacheRepo = new LocalCacheRepo(this, cc);
        //Same blackout and include/exclude settings for the cache
        localCacheRepo.setBlackedOut(isBlackedOut());
        localCacheRepo.setIncludesPattern(getIncludesPattern());
        localCacheRepo.setExcludesPattern(getExcludesPattern());
        if (retrievalCachePeriodSecs > 0) {
            LOGGER.info(this + ": Retrieval cache will be enabled with period of "
                    + retrievalCachePeriodSecs + " seconds");
        } else {
            LOGGER.info(this + ": Retrieval cache will be disbaled.");
        }
        if (failedRetrievalCachePeriodSecs > 0) {
            LOGGER.info(this + ": Enabling failed retrieval cache with period of "
                    + failedRetrievalCachePeriodSecs + " seconds");
            failedRetrievalsCache =
                    new DefaultRetrievalCache(failedRetrievalCachePeriodSecs * 1000);
        } else {
            LOGGER.info(this + ": Disabling failed retrieval cache");
        }
        if (missedRetrievalCachePeriodSecs > 0) {
            LOGGER.info(this + ": Enabling misses retrieval cache with period of "
                    + missedRetrievalCachePeriodSecs + " seconds");
            missedRetrievalsCache =
                    new DefaultRetrievalCache(missedRetrievalCachePeriodSecs * 1000);
        } else {
            LOGGER.info(this + ": Disabling misses retrieval cache");
        }
    }

    @XmlElement(required = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isLocal() {
        return false;
    }

    public boolean isCache() {
        return false;
    }

    /**
     * if a repository is set to hard fail, then the download engine will terminate the whole
     * download process (with a status 500) if any of the repositories have unexpected errors.
     */
    @XmlElement(defaultValue = "false", required = false)
    public boolean isHardFail() {
        return hardFail;
    }

    @XmlElement(defaultValue = "43200", required = false)
    public long getRetrievalCachePeriodSecs() {
        return retrievalCachePeriodSecs;
    }

    @XmlElement(defaultValue = "30", required = false)
    public long getFailedRetrievalCachePeriodSecs() {
        return failedRetrievalCachePeriodSecs;
    }

    @XmlElement(defaultValue = "43200", required = false)
    public long getMissedRetrievalCachePeriodSecs() {
        return missedRetrievalCachePeriodSecs;
    }

    /**
     * If a file repository is set to "store" mode, it will copy the found files into the main
     * repository store.
     */
    @XmlElement(defaultValue = "true", required = false)
    public boolean isStoreArtifactsLocally() {
        return storeArtifactsLocally;
    }

    public void setHardFail(boolean hardFail) {
        this.hardFail = hardFail;
    }

    public void setRetrievalCachePeriodSecs(long retrievalCachePeriodSecs) {
        this.retrievalCachePeriodSecs = retrievalCachePeriodSecs;
    }

    public void setFailedRetrievalCachePeriodSecs(long failedRetrievalCachePeriodSecs) {
        this.failedRetrievalCachePeriodSecs = failedRetrievalCachePeriodSecs;
    }

    public void setMissedRetrievalCachePeriodSecs(long missedRetrievalCachePeriodSecs) {
        this.missedRetrievalCachePeriodSecs = missedRetrievalCachePeriodSecs;
    }

    public void setStoreArtifactsLocally(boolean storeArtifactsLocally) {
        this.storeArtifactsLocally = storeArtifactsLocally;
    }

    /**
     * Retrieve the (metadata) information about the artifact, unless still cahced as failure or
     * miss.
     *
     * @param path the artifact's path
     * @return A repository resource updated with the uptodate metadata
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    public final RepoResource getInfo(String path) {
        //Skip if in blackout or not accepting
        if (isBlackedOut() || !accept(path)) {
            return new NotFoundRepoResource(path, this);
        }
        RepoResource res;
        synchronized (failedRetrievalsCache) {
            try {
                //Try to get it from the caches
                res = failedRetrievalsCache.getResource(path);
                synchronized (missedRetrievalsCache) {
                    if (res == null) {
                        res = missedRetrievalsCache.getResource(path);
                    }
                    if (res == null) {
                        //Try to get it from the remote repository
                        res = retrieveInfo(path);
                        if (!res.isFound()) {
                            //Update the non-found cache for a miss
                            LOGGER.info(this + ": " + res + " not found at '" + path + "'.");
                            if (missedRetrievalCachePeriodSecs > 0) {
                                missedRetrievalsCache.setResource(res);
                            }
                        }
                    } else if (!res.isFound()) {
                        LOGGER.info(this + ": " + res + " cached as not found at '" + path + "'.");
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(this + ": " + res + " retrieved at '" + path + "'.");
                        }
                    }
                }
                return res;
            } catch (Exception e) {
                LOGGER.warn(this + ": Error in getting information for '" + path +
                        "' (" + e.getMessage() + ").");
                //Update the non-found cache for a failure
                res = new NotFoundRepoResource(path, this);
                if (failedRetrievalCachePeriodSecs > 0) {
                    failedRetrievalsCache.setResource(res);
                }
                if (isHardFail()) {
                    throw new RepoAccessException(this, path, e.getLocalizedMessage(), e);
                }
            }
        }
        //If we cannot get the resource remotely and an expired cache entry exists use it by
        //unexpiring it
        if (!res.isFound() && storeArtifactsLocally) {
            if (localCacheRepo.fileNodeExists(path)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(this +
                            ": falling back to using expired cache entry for resource info at '" +
                            path + "'.");
                }
                localCacheRepo.unexpire(path);
                res = localCacheRepo.getInfo(path);
            }
        }
        return res;
    }

    public LocalCacheRepo getLocalCacheRepo() {
        return localCacheRepo;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException {
        String path = res.getRelPath();
        if (storeArtifactsLocally) {
            try {
                RepoResource targetRes = localCacheRepo.retrieveInfo(path);
                //Retrieve remotely only if locally cached artifact is older than remote one
                if (!targetRes.isFound() ||
                        res.getLastModified().after(targetRes.getLastModified())) {
                    ResourceStreamHandle handle = retrieveResource(path);
                    try {
                        LOGGER.info("Copying " + path + " from " + this + " to " + localCacheRepo);
                        //Create/override the resource in the storage cache
                        InputStream is = handle.getInputStream();
                        localCacheRepo.saveResource(res, is);
                    } finally {
                        handle.close();
                    }
                }
                //Unexpire the resource and remove it from retrieval caches
                localCacheRepo.unexpire(path);
                removeFromCaches(path);
                return localCacheRepo.getResourceStreamHandle(targetRes);
            } catch (IOException e) {
                //If we fail on remote fetching and we can get the resource from an expired entry in
                //the local cache - fallback to using it, else rethrow the exception
                if (localCacheRepo.fileNodeExists(path)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(this +
                                ": falling back to using expired cache entry for resource at '" +
                                path + "'.");
                    }
                    localCacheRepo.unexpire(path);
                    RepoResource targetRes = localCacheRepo.retrieveInfo(path);
                    return localCacheRepo.getResourceStreamHandle(targetRes);
                } else {
                    throw e;
                }
            }
        } else {
            ResourceStreamHandle handle = retrieveResource(path);
            return handle;
        }
    }

    public void clearCaches() {
        if (failedRetrievalsCache != null) {
            failedRetrievalsCache.clear();
        }
        if (missedRetrievalsCache != null) {
            missedRetrievalsCache.clear();
        }
    }

    public void removeFromCaches(String path) {
        //Update the caches
        failedRetrievalsCache.removeResource(path);
        missedRetrievalsCache.removeResource(path);
    }
}
