package org.artifactory.ui.rest.model.admin.configuration.docker.repo;

import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;

/**
 * @author Shay Yaakov
 */
public class DockerRepo implements RestModel {

    private String hostname;
    private Boolean deployToLocal;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Boolean getDeployToLocal() {
        return deployToLocal;
    }

    public void setDeployToLocal(Boolean deployToLocal) {
        this.deployToLocal = deployToLocal;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
