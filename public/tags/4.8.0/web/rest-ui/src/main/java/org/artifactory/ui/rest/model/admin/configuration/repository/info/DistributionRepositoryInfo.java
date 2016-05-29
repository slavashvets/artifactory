package org.artifactory.ui.rest.model.admin.configuration.repository.info;

import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;

/**
 * @author Dan Feldman
 */
public class DistributionRepositoryInfo extends RepositoryInfo {

    private String visibility;

    public DistributionRepositoryInfo() {
    }

    public DistributionRepositoryInfo(DistributionRepoDescriptor descriptor) {
        repoKey = descriptor.getKey();
        repoType = descriptor.getType().toString();
        hasReindexAction = false;
        visibility = descriptor.getDefaultNewRepoPrivate() ? "Private" : "Public";
    }

    public String getVisibility() {
        return visibility;
    }
}
