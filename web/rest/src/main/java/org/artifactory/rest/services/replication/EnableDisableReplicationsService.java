package org.artifactory.rest.services.replication;

import org.artifactory.api.rest.replication.ReplicationEnableDisableRequest;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.replication.ReplicationBaseDescriptor;
import org.artifactory.rest.http.request.ArtifactoryRestRequest;
import org.artifactory.rest.http.response.IResponse;
import org.artifactory.util.PathMatcher;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EnableDisableReplicationsService extends BaseReplicationService {

    @Override
    public void execute(ArtifactoryRestRequest artifactoryRequest, IResponse artifactoryResponse) {
        ReplicationEnableDisableRequest request = (ReplicationEnableDisableRequest) artifactoryRequest.getImodel();
        // validate pro license
        verifyArtifactoryPro();

        CentralConfigDescriptor descriptor = centralConfigService.getMutableDescriptor();
        handleLocalReplications(request, descriptor);
        handleRemoteReplications(request, descriptor);
        centralConfigService.saveEditedDescriptorAndReload(descriptor);
        artifactoryResponse.setResponseCode(HttpServletResponse.SC_CREATED);
    }

    private void handleLocalReplications(ReplicationEnableDisableRequest request, CentralConfigDescriptor descriptor) {
        descriptor.getLocalReplications().stream()
                .forEach(replication -> enableOrDisable(replication, replication.getUrl(), request));
    }

    private void handleRemoteReplications(ReplicationEnableDisableRequest request, CentralConfigDescriptor descriptor) {
        descriptor.getRemoteReplications().stream()
                .forEach(replication -> enableOrDisable(replication, getRemoteUrl(descriptor, replication.getRepoKey()), request));
    }

    private String getRemoteUrl(CentralConfigDescriptor descriptor, String repoKey) {
        return descriptor.getRemoteRepositoriesMap().get(repoKey).getUrl();
    }

    private void enableOrDisable(ReplicationBaseDescriptor replication, String url, ReplicationEnableDisableRequest request) {
        if (PathMatcher.matches(url, request.getInclude(), request.getExclude(), false)) {
            replication.setEnabled(request.isEnable());
        }
    }
}
