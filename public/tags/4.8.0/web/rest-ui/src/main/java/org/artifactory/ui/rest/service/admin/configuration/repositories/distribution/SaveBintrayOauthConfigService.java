package org.artifactory.ui.rest.service.admin.configuration.repositories.distribution;

import com.sun.jersey.core.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.bintray.distribution.model.DistributionRepoCreationDetails;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.configuration.repository.typespecific.DistRepoTypeSpecificConfigModel;
import org.artifactory.ui.rest.service.admin.configuration.repositories.util.exception.RepoConfigException;
import org.artifactory.util.AlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SaveBintrayOauthConfigService implements RestService<DistRepoTypeSpecificConfigModel> {

    @Autowired
    DistributionService distributionService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        DistRepoTypeSpecificConfigModel distModel = (DistRepoTypeSpecificConfigModel) request.getImodel();

        DistributionRepoCreationDetails creationDetails = null;
        try {
            creationDetails = createBintrayAppConfig(distModel);
            //Messed up response but no exception - shouldn't happen.
            if (!response.isFailed()
                    && (creationDetails == null || StringUtils.isBlank(creationDetails.oauthAppConfigKey))) {
                response.error("Failed to establish trust with Bintray, check the logs for more info.")
                        .responseCode(SC_BAD_REQUEST);
            }
        } catch (RepoConfigException rce) {
            response.error(rce.getMessage()).responseCode(rce.getStatusCode());
        }
        if (response.isFailed() || creationDetails == null) {
            return;
        }
        //We just need to return the key of the newly created OAuth app
        DistRepoTypeSpecificConfigModel newModel = new DistRepoTypeSpecificConfigModel();
        newModel.setBintrayAppConfig(creationDetails.oauthAppConfigKey);
        newModel.setPremium(creationDetails.isOrgPremium);
        newModel.setAvailableLicenses(creationDetails.orgLicenses);
        newModel.setOrg(creationDetails.org);
        newModel.setClientId(creationDetails.clientId);
        response.iModel(newModel);
    }

    private DistributionRepoCreationDetails createBintrayAppConfig(DistRepoTypeSpecificConfigModel distModel)
            throws RepoConfigException {
        String[] clientIdAndSecret = validateDistributionRepoParams(distModel);
        try {
            return distributionService.createBintrayAppConfig(clientIdAndSecret[0], clientIdAndSecret[1],
                    distModel.getCode(), distModel.getScope(), distModel.getRedirectUrl());
        } catch (IOException ioe) {
            throw new RepoConfigException("Error executing OAuth token creation request: " + ioe.getMessage(),
                    SC_BAD_REQUEST, ioe);
        } catch (AlreadyExistsException aee) {
            throw new RepoConfigException(aee.getMessage(), SC_BAD_REQUEST);
        }
    }

    /**
     * @return the client id and secret returned from bintray in the {@param distModel}'s getBintrayAuthString()
     * @throws RepoConfigException
     */
    private String[] validateDistributionRepoParams(DistRepoTypeSpecificConfigModel distModel)
            throws RepoConfigException {
        if (StringUtils.isBlank(distModel.getBintrayAuthString())) {
            throw new RepoConfigException("Bintray authorization code provided is valid. Try to authorize again", SC_BAD_REQUEST);
        } else if (StringUtils.isBlank(distModel.getParamClientId())) {
            throw new RepoConfigException("The Bintray client id parameter is empty. Try to authorize again",
                    SC_BAD_REQUEST);
        } else if (StringUtils.isBlank(distModel.getCode())) {
            throw new RepoConfigException("The Bintray code parameter is empty. Try to authorize again",
                    SC_BAD_REQUEST);
        } else if (StringUtils.isBlank(distModel.getRedirectUrl())) {
            throw new RepoConfigException("The redirect url parameter is empty. Try to authorize again", SC_BAD_REQUEST);
        } else if (StringUtils.isBlank(distModel.getScope())) {
            throw new RepoConfigException("The Bintray scope parameter is empty. Try to authorize again",
                    SC_BAD_REQUEST);
        }

        String decodedAuthString = Base64.base64Decode(distModel.getBintrayAuthString());
        //Auth String is client_id:<client_id>:client_secret:<client_secret>
        String[] clientIdAndSecret = decodedAuthString.split(":");
        if (!(clientIdAndSecret.length == 2)) {
            throw new RepoConfigException("An invalid authentication string was pasted in the text box.", SC_BAD_REQUEST);
        } else if (!clientIdAndSecret[0].equals(distModel.getParamClientId())) {
            throw new RepoConfigException("There is a mismatch between the client ID you pasted in the box and the " +
                    "one returned from Bintray. This can pose a security risk.", SC_BAD_REQUEST);
        }
        return clientIdAndSecret;
    }
}
