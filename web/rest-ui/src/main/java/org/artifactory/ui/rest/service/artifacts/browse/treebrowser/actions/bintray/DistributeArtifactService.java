package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.actions.bintray;

import org.apache.http.HttpStatus;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.aql.util.AqlSearchablePath;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.rest.common.model.artifact.BaseArtifact;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.artifactory.ui.rest.service.utils.distribution.DistributionUIResponseUtils.createResponseEntity;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DistributeArtifactService implements RestService {

    @Autowired
    DistributionService distributionService;

    @Autowired
    RepositoryService repoService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        // boolean isDryRun = Boolean.parseBoolean(request.getQueryParamByKey("dryRun")); //TODO [by dan]:  handle dry run
        boolean async = Boolean.parseBoolean(request.getQueryParamByKey("async"));
        boolean publish = Boolean.parseBoolean(request.getQueryParamByKey("publish"));
        boolean overrideExistingFiles = Boolean.parseBoolean(request.getQueryParamByKey("overrideExistingFiles"));
        String targetRepo = request.getQueryParamByKey("targetRepo");
        BaseArtifact artifact = (BaseArtifact) request.getImodel();
        RepoPath artifactRepoPath = RepoPathFactory.create(artifact.getRepoKey() + "/" + artifact.getPath());
        if (!repoService.exists(artifactRepoPath)) {
            response.error("No such path " + artifactRepoPath.toPath()).responseCode(HttpStatus.SC_NOT_FOUND);
            return;
        }
        Distribution distribution = new Distribution();
        if (repoService.getItemInfo(artifactRepoPath).isFolder()) {
            getPathsForCurrentFolderAndSubFolders(artifactRepoPath).forEach(distribution::addPath);
        } else {
            distribution.addPath(artifactRepoPath.toPath());
        }
        distribution.setTargetRepo(targetRepo);
        distribution.setAsync(async);
        // distribution.setDryRun(isDryRun); //TODO [by dan]:  handle dry run
        distribution.setPublish(publish);
        distribution.setOverrideExistingFiles(overrideExistingFiles);
        BasicStatusHolder status = distributionService.distribute(distribution);
        response.iModel(createResponseEntity(artifactRepoPath.toPath(), targetRepo, status));
    }

    private List<String> getPathsForCurrentFolderAndSubFolders(RepoPath artifactRepoPath) {
        return AqlUtils.getSearchablePathForCurrentFolderAndSubfolders(artifactRepoPath).stream()
                .map(AqlSearchablePath::toRepoPath)
                .map(RepoPath::toPath)
                .collect(Collectors.toList());
    }
}
