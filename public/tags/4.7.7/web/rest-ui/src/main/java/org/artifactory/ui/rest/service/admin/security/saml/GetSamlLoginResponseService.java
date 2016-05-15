package org.artifactory.ui.rest.service.admin.security.saml;

import com.sun.jersey.spi.container.ContainerRequest;
import org.artifactory.addon.sso.saml.SamlHandler;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetSamlLoginResponseService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetSamlLoginResponseService.class);
    @Autowired
    private SamlHandler samlHandler;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        try {
            HashMap<String, List<String>> formParameters = ((ContainerRequest) request.getRequest()).getFormParameters();
            samlHandler.handleLoginResponse(request.getServletRequest(), response.getServletResponse(), formParameters);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.error(e.getMessage());
        }
    }
}


//.get("SAMLResponse").get(0);