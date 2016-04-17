package org.artifactory.ui.rest.service.admin.advanced.replication;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.replication.GlobalReplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author gidis
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetGlobalReplicationsConfigService implements RestService<GlobalReplicationConfig> {
    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest<GlobalReplicationConfig> request, RestResponse response) {
        GlobalReplicationConfig model = new GlobalReplicationConfig();
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        model.setBlockPullReplications(descriptor.getReplicationsConfig().isBlockPullReplications());
        model.setBlockPushReplications(descriptor.getReplicationsConfig().isBlockPushReplications());
        response.iModel(model);
    }
}
