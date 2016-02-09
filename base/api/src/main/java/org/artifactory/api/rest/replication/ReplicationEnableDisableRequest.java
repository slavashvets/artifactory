package org.artifactory.api.rest.replication;

import org.artifactory.api.rest.restmodel.IModel;

import java.io.Serializable;
import java.util.List;

/**
 * @author Shay Yaakov
 */
public class ReplicationEnableDisableRequest implements Serializable, IModel {

    private List<String> include;
    private List<String> exclude;
    private boolean isEnable;

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }
}
