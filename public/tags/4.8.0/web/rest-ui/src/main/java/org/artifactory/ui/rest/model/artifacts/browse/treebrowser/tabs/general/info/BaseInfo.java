package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.info;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.addon.watch.ArtifactWatchAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.replication.ReplicationStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.fs.WatcherInfo;
import org.artifactory.fs.WatchersInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.Pair;

/**
 * @author Chen Keinan
 */
public class BaseInfo {

    protected final String SLASH = "/";
    private String name;
    private String repoType;
    private String repositoryPath;
    private String externalUrl;
    private Boolean smartRepo;
    private Boolean remoteDeleted;
    private String bintrayUrl;
    private String distPackageType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public Boolean getRemoteDeleted() {
        return remoteDeleted;
    }

    public void setRemoteDeleted(Boolean remoteDeleted) {
        this.remoteDeleted = remoteDeleted;
    }

    public String getBintrayUrl() {
        return bintrayUrl;
    }

    public void setBintrayUrl(String bintrayUrl) {
        this.bintrayUrl = bintrayUrl;
    }

    public String getDistPackageType() {
        return distPackageType;
    }

    public void setDistPackageType(String distPackageType) {
        this.distPackageType = distPackageType;
    }

    /**
     * get watching since for specific repo path if exist
     *
     * @param userName - user watching
     * @return - if not null - watching since in pretty date format
     */
    protected String fetchWatchingSince(String userName, RepoPath repoPath) {
        String watchingSince = null;
        ArtifactWatchAddon artifactWatchAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(
                ArtifactWatchAddon.class);
        Pair<RepoPath, WatchersInfo> nearestWatchFound = artifactWatchAddon.getNearestWatchDefinition(repoPath,
                userName);
        if(nearestWatchFound != null) {
            WatchersInfo watchers = nearestWatchFound.getSecond();
            if (watchers != null) {
                WatcherInfo watcher = watchers.getWatcher(userName);
                if (watcher != null) {
                    CentralConfigService centralConfig = ContextHelper.get().getCentralConfig();
                    watchingSince = centralConfig.format(watcher.getWatchingSinceTime());

                }
            }
        }

        return watchingSince;
    }

    /**
     * get last replication status for repo path if replication is enable
     *
     * @param repoPath - repository path
     * @return - last replication status
     */
    protected String getLastReplicationInfo(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        LocalRepoDescriptor repoDescriptor = repositoryService.localCachedOrDistributionRepoDescriptorByKey(repoKey);
        final boolean isCache = repoDescriptor.isCache();
        AuthorizationService authorizationService = ContextHelper.get().getAuthorizationService();
        boolean isAdmin = authorizationService.isAdmin();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        String lastReplicationStatus = null;
        if (isAdmin) {
            ReplicationAddon replicationAddon = addonsManager.addonByType(ReplicationAddon.class);
            ReplicationStatus replicationStatus = replicationAddon.getReplicationStatus(repoPath);
            if (replicationStatus != null && isReplicationEnabled(isCache, repoKey)) {
                lastReplicationStatus = replicationStatus.getDisplayName();
            }
        }
        return lastReplicationStatus;
    }

    /**
     * check weather replication is enable
     *
     * @return - if true - replication is enable
     */
    private boolean isReplicationEnabled(boolean isCache, String repoKey) {
        CentralConfigService centralConfigService = ContextHelper.get().getCentralConfig();
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        if (isCache) {
            return isRemoteReplicationEnabled(descriptor, repoKey);
        } else {
            return isLocalReplicationEnabled(descriptor, repoKey);
        }
    }

    private boolean isRemoteReplicationEnabled(CentralConfigDescriptor descriptor, String repoKey) {
        String remoteRepoKey = StringUtils.remove(repoKey, LocalCacheRepoDescriptor.PATH_SUFFIX);
        RemoteReplicationDescriptor remoteReplication = descriptor.getRemoteReplication(remoteRepoKey);
        boolean blockPullReplications = descriptor.getReplicationsConfig().isBlockPullReplications();
        return remoteReplication != null && remoteReplication.isEnabled() && ! blockPullReplications;
    }

    private boolean isLocalReplicationEnabled(CentralConfigDescriptor descriptor, String repoKey) {
        LocalReplicationDescriptor localReplication = descriptor.getLocalReplication(repoKey);
        boolean blockPushReplications = descriptor.getReplicationsConfig().isBlockPushReplications();
        return localReplication != null && localReplication.isEnabled() && !blockPushReplications;
    }

    public String getRepoType() {
        return repoType;
    }

    public void setRepoType(String repoType) {
        this.repoType = repoType;
    }

    public void setSmartRepo(Boolean smartRepo) {
        this.smartRepo = smartRepo;
    }

    public Boolean isSmartRepo() {
        return smartRepo;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }
}
