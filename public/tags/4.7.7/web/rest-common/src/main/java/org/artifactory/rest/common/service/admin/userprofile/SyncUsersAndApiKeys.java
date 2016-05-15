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

import java.util.List;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SyncUsersAndApiKeys implements RestService {

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    ApiKeyManager apiKeyManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        if (authorizationService.isAdmin()) {
            List<UserProfileModel> userProfileModels = request.getModels();
            StringBuilder syncErrorBuilder = new StringBuilder();
            userProfileModels.forEach(userProfileModel -> {
                TokenKeyValue updatedToken = apiKeyManager.updateToken(userProfileModel.getUserName(), userProfileModel.getApiKey());
                if (updatedToken == null) {
                    syncErrorBuilder.append("Error while syncing api key for user: ")
                            .append(userProfileModel.getUserName()).append("\n");
                }
            });
            if (StringUtils.isEmpty(syncErrorBuilder.toString())) {
                response.error(syncErrorBuilder.toString());
            }
        }
    }
}
