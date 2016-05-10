package org.artifactory.ui.rest.service.admin.configuration.general;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.security.sshserver.SshServerSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.configuration.generalconfig.GeneralConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

/**
 * @author Chenk
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetGeneralConfigService implements RestService<GeneralConfig> {

    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest<GeneralConfig> request, RestResponse response) {
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        // update general config
        GeneralConfig generalconfig = new GeneralConfig(mutableDescriptor);
        generalconfig.setForceBaseUrl(isForceBaseUrl(mutableDescriptor));
        response.iModel(generalconfig);
    }

    private boolean isForceBaseUrl(MutableCentralConfigDescriptor mutableDescriptor) {
        Stream<? extends RepoBaseDescriptor> local = mutableDescriptor.getLocalRepositoriesMap().values().stream();
        Stream<? extends RepoBaseDescriptor> remote = mutableDescriptor.getRemoteRepositoriesMap().values().stream();
        Stream<? extends RepoBaseDescriptor> virtual = mutableDescriptor.getVirtualRepositoriesMap().values().stream();
        Stream<RepoBaseDescriptor> concat = concat(local, concat(remote, virtual));
        SshServerSettings sshServerSettings = mutableDescriptor.getSecurity().getSshServerSettings();
        boolean activeSshServer = sshServerSettings != null && sshServerSettings.isEnableSshServer();
        boolean cocoaPodsRepoExist = concat.anyMatch(descriptor -> RepoType.CocoaPods.equals(descriptor.getType()));
        return cocoaPodsRepoExist || activeSshServer;
    }

}
