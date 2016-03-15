package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.VcsGitConfiguration;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues;

import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_PODS_SPECS_REPO;
import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_VCS_GIT_CONFIG;

/**
 * @author Dan Feldman
 */
public class CocoaPodsTypeSpecificConfigModel extends VcsTypeSpecificConfigModel {

    //remote
    private String specsRepoUrl = DEFAULT_PODS_SPECS_REPO;
    private VcsGitConfiguration specsRepoProvider = DEFAULT_VCS_GIT_CONFIG;

    //TODO [by dan]: ?
    //virtual
    //private Boolean enableExternalDependencies = false;
    //private List<String> externalPatterns = Lists.newArrayList("**");
    //private String externalRemoteRepo = "";

    public String getSpecsRepoUrl() {
        return specsRepoUrl;
    }

    public void setSpecsRepoUrl(String specsRepoUrl) {
        this.specsRepoUrl = specsRepoUrl;
    }

    public VcsGitConfiguration getSpecsRepoProvider() {
        return specsRepoProvider;
    }

    public void setSpecsRepoProvider(VcsGitConfiguration specsRepoProvider) {
        this.specsRepoProvider = specsRepoProvider;
    }

    /*  public Boolean getEnableExternalDependencies() {
        return enableExternalDependencies;
    }

    public void setEnableExternalDependencies(Boolean enableExternalDependencies) {
        this.enableExternalDependencies = enableExternalDependencies;
    }

    public List<String> getExternalPatterns() {
        return externalPatterns;
    }

    public void setExternalPatterns(List<String> externalPatterns) {
        this.externalPatterns = externalPatterns;
    }

    public String getExternalRemoteRepo() {
        return externalRemoteRepo;
    }

    public void setExternalRemoteRepo(String externalRemoteRepo) {
        this.externalRemoteRepo = externalRemoteRepo;
    }*/

    @Override
    public RepoType getRepoType() {
        return RepoType.CocoaPods;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.VCS_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
