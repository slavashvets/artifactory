package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.actions;

import org.apache.http.HttpStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex.BaseIndexCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RecalculateIndexService<T extends BaseIndexCalculator> implements RestService<T> {
    private static final Logger log = LoggerFactory.getLogger(RecalculateIndexService.class);

    @Autowired
    AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        BaseIndexCalculator baseIndexCalculator = request.getImodel();
        RepoPath repoPath = RepoPathFactory.create(baseIndexCalculator.getRepoKey());
        if (repoPath != null && !authorizationService.canManage(repoPath)) {
            response.error("Forbidden").responseCode(HttpStatus.SC_FORBIDDEN);
            return;
        }

        try {
            // calculate index for repo type
            baseIndexCalculator.calculateIndex();
            String message = "Recalculating index for repository " + baseIndexCalculator.getRepoKey() + " scheduled to run";
            log.info(message);
            response.info(message);
        } catch (Exception e) {
            response.error("failed to schedule index calculation");
        }
    }
}
