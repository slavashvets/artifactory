package org.artifactory.ui.rest.model.setmeup;

import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.ui.rest.model.utils.repositories.RepoKeyType;

import java.util.List;

/**
 * @author Chen Keinan
 */
public class SetMeUpModel extends BaseModel {

    private List<RepoKeyType> repoKeyTypes;

    public List<RepoKeyType> getRepoKeyTypes() {
        return repoKeyTypes;
    }

    private String baseUrl;

    private String serverId;

    private String hostname;

    public void setRepoKeyTypes(List<RepoKeyType> repoKeyTypes) {
        this.repoKeyTypes = repoKeyTypes;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
