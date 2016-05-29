package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.actions.bintray;

import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetAvailableDistributionReposService implements RestService {

    @Autowired
    RepositoryService repoService;

    @Autowired
    AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        response.iModelList(repoService.getDistributionRepoDescriptors().stream()
                .map(RepoDescriptor::getKey)
                .map(RepoPathFactory::create)
                .filter(authorizationService::canDeploy)
                .collect(Collectors.toList()));
    }
}
