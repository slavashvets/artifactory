package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.info;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.BintrayApplicationConfig;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;
import org.artifactory.util.distribution.DistributionConstants;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * @author Chen Keinan
 */
@JsonPropertyOrder(
    {
        "name", "repositoryPath", "distPackageType", "bintrayUrl",
        "deployedBy", "artifactsCount", "created", "watchingSince", "lastReplicationStatus"
    }
)
public class FolderInfo extends BaseInfo {

    private String deployedBy;
    private String created;
    private String watchingSince;
    private String lastReplicationStatus;
    private int artifactsCount;
    private String distPackageType;

    public String getDeployedBy() {
        return deployedBy;
    }

    public void setDeployedBy(String deployedBy) {
        this.deployedBy = deployedBy;
    }

    /**
     * populate folder info data
     *
     * @param repoService          -repository service
     * @param repoPath             - repo path
     * @param centralConfigService - central config service
     * @return
     */
    public void populateFolderInfo(RepositoryService repoService, RepoPath repoPath,
                                   CentralConfigService centralConfigService, String userName) {
        // set name
        this.setName(repoPath.getName());
        // set repository path
        this.setRepositoryPath(repoPath.getRepoKey() + SLASH + repoPath.getPath() + SLASH);
        ItemInfo itemInfo = repoService.getItemInfo(repoPath);
        // set watching since
        setWatchingSince(fetchWatchingSince(userName, repoPath));
        // set created
        setCreated(centralConfigService, itemInfo);
        // set deployed by
        this.setDeployedBy(itemInfo.getModifiedBy());
        // set last replication status
        setLastReplicationStatus(getLastReplicationInfo(repoPath));
        setBintrayUrl(fetchBintrayUrl(repoService, repoPath));
        setDistPackageType(fetchDistributionPackageType(repoService, repoPath));
    }

    private String fetchDistributionPackageType(RepositoryService repoService, RepoPath repoPath) {
        LocalRepoDescriptor repoDescriptor = repoService.localCachedOrDistributionRepoDescriptorByKey(repoPath.getRepoKey());
        if (repoDescriptor != null && repoDescriptor instanceof DistributionRepoDescriptor) {
            Properties properties = repoService.getProperties(repoPath);
            if (properties != null) {
                return properties.getFirst(DistributionConstants.ARTIFACT_TYPE_OVERRIDE_PROP);
            }
        }
        return null;
    }

    private String fetchBintrayUrl(RepositoryService repoService, RepoPath repoPath) {
        LocalRepoDescriptor repoDescriptor = repoService.localCachedOrDistributionRepoDescriptorByKey(repoPath.getRepoKey());
        if (repoDescriptor != null && repoDescriptor instanceof DistributionRepoDescriptor) {
            DistributionRepoDescriptor descriptor = (DistributionRepoDescriptor) repoDescriptor;
            BintrayApplicationConfig bintrayApplication = descriptor.getBintrayApplication();
            if (bintrayApplication != null) {
                String path = repoPath.getPath();
                boolean isFileOrFolder = PathUtils.getPathElements(path).length > 3;
                if (!isFileOrFolder) {
                    return ConstantValues.bintrayUrl.getString() + "/" + bintrayApplication.getOrg() + "/" + path;
                }
            }
        }
        return null;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getWatchingSince() {
        return watchingSince;
    }

    public void setWatchingSince(String watchingSince) {
        this.watchingSince = watchingSince;
    }

    public String getLastReplicationStatus() {
        return lastReplicationStatus;
    }

    public void setLastReplicationStatus(String lastReplicationStatus) {
        this.lastReplicationStatus = lastReplicationStatus;
    }

    public int getArtifactsCount() {
        return artifactsCount;
    }

    public void setArtifactsCount(int artifactsCount) {
        this.artifactsCount = artifactsCount;
    }

    @Override
    public String getDistPackageType() {
        return distPackageType;
    }

    @Override
    public void setDistPackageType(String distPackageType) {
        this.distPackageType = distPackageType;
    }

    /**
     * @param item
     */
    public void populateVirtualRemoteFolderInfo(BaseBrowsableItem item) {
        CentralConfigService centralConfig = ContextHelper.get().getCentralConfig();
        // set name
        this.setName(item.getName());
        // set repository path
        this.setRepositoryPath(item.getRepoKey() + SLASH + item.getRelativePath() + SLASH);

        if (!item.isRemote()) {
            this.setCreated(centralConfig.format(item.getCreated()));
        }
    }

    /**
     * set created data
     *
     * @param centralConfigService - central configuration service
     * @param itemInfo             - item info
     */
    private void setCreated(CentralConfigService centralConfigService, ItemInfo itemInfo) {
        String created = centralConfigService.format(itemInfo.getCreated()) + " " + DurationFormatUtils.formatDuration(
                System.currentTimeMillis() - itemInfo.getCreated(), "(d'd' H'h' m'm' s's' ago)");
        this.setCreated(created);
    }

}
