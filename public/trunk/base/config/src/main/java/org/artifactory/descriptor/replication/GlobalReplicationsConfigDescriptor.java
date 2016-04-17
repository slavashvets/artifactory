package org.artifactory.descriptor.replication;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlType;

/**
 * @author gidis
 */
@XmlType(name = "replicationsConfigType", propOrder = {"blockPushReplications", "blockPullReplications"}, namespace = Descriptor.NS)
public class GlobalReplicationsConfigDescriptor implements Descriptor {

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
