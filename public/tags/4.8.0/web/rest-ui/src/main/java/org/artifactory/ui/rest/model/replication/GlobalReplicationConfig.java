package org.artifactory.ui.rest.model.replication;

import org.artifactory.rest.common.model.BaseModel;

/**
 * @author gidis
 */
public class GlobalReplicationConfig extends BaseModel {
    private boolean blockPullReplications;

    private boolean blockPushReplications;

    public boolean isBlockPullReplications() {
        return blockPullReplications;
    }

    public void setBlockPullReplications(boolean blockPullReplications) {
        this.blockPullReplications = blockPullReplications;
    }

    public boolean isBlockPushReplications() {
        return blockPushReplications;
    }

    public void setBlockPushReplications(boolean blockPushReplications) {
        this.blockPushReplications = blockPushReplications;
    }
}
