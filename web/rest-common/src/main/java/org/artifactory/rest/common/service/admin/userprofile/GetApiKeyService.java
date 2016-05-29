package org.artifactory.rest.common.service.admin.userprofile;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.model.userprofile.UserProfileModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetApiKeyService implements RestService {
    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    ApiKeyManager apiKeyManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {

        if (authorizationService.isAnonymous()) {
            return;
        }
        String userName;
        String id = request.getPathParamByKey("id");
        if (!StringUtils.isEmpty(id) && authorizationService.isAdmin()) {
            userName = id;
        } else {
            userName = authorizationService.currentUsername();
        }
        TokenKeyValue token = apiKeyManager.getToken(userName);
        UserProfileModel userProfileModel;
        if (token != null) {
            userProfileModel = new UserProfileModel(token.getToken());
        } else {
            userProfileModel = new UserProfileModel();
        }
        response.iModel(userProfileModel);
    }
}
