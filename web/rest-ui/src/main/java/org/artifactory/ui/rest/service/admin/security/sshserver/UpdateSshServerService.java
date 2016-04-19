package org.artifactory.ui.rest.service.admin.security.sshserver;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.sshserver.SshServerSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.util.AolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSshServerService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AolUtils.assertNotAol("UpdateSshServer");
        MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
        SecurityDescriptor securityDescriptor = centralConfig.getSecurity();
        SshServerSettings sshServerSettings = (SshServerSettings) request.getImodel();
        saveSshSettings(centralConfig, securityDescriptor, sshServerSettings);
        response.info("Successfully updated SSH server settings");
    }

    private void saveSshSettings(MutableCentralConfigDescriptor centralConfig, SecurityDescriptor securityDescriptor,
                                 SshServerSettings sshServerSettings) {
        securityDescriptor.setSshServerSettings(sshServerSettings);
        centralConfigService.saveEditedDescriptorAndReload(centralConfig);
    }
}
