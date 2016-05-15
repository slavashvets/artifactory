package org.artifactory.ui.rest.service.builds.bintray;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.docker.DockerAddon;
import org.artifactory.api.bintray.BintrayParams;
import org.artifactory.api.bintray.docker.BintrayDockerPushRequest;
import org.artifactory.common.ConstantValues;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.ui.rest.model.builds.BintrayModel;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.governance.AbstractBuildService;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PushDockerTagToBintrayService extends AbstractBuildService {
    private static final Logger log = LoggerFactory.getLogger(PushDockerTagToBintrayService.class);

    @Autowired
    AddonsManager addonsManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        BintrayModel bintrayModel = (BintrayModel) request.getImodel();
        BintrayParams bintrayParams = bintrayModel.getBintrayParams();
        try {
            String repoKey = request.getQueryParamByKey("repoKey");
            String path = request.getQueryParamByKey("path");
            RepoPath repoPath = InternalRepoPathFactory.create(repoKey, path);
            push(repoPath, bintrayParams, response);
        } catch (Exception e) {
            String msg = e.toString();
            if (msg.contains("is not a docker registry")) {
                response.error("Repository " + bintrayParams.getRepo() + " is not a Docker V2 registry");
            } else {
                response.error("Error reported during the push. Please review the logs for further information");
            }
            log.error(msg, e);
        }
    }

    private void push(RepoPath repoPath, BintrayParams bintrayParams, RestResponse response) {
        String[] subjectRepo = StringUtils.split(bintrayParams.getRepo(), "/");
        BintrayDockerPushRequest request = new BintrayDockerPushRequest();
        request.async = true;
        request.dockerTagName = PathUtils.getLastPathElement(repoPath.getPath());
        request.dockerRepository = PathUtils.trimTrailingSlashes(StringUtils.removeEnd(repoPath.getPath(), request.dockerTagName));
        request.bintraySubject = subjectRepo[0];
        request.bintrayRepo = subjectRepo[1];
        addonsManager.addonByType(DockerAddon.class).pushTagToBintray(repoPath.getRepoKey(), request);

        String escapedImageUrl = ConstantValues.bintrayUrl.getString() +
                "/" + request.bintraySubject +
                "/" + request.bintrayRepo +
                "/" + request.dockerRepository.replace( "/", "%3A") +
                "/" + request.dockerTagName + "/view";

        response.info("Pushing " + "<a href=\"" + escapedImageUrl + "\" target=\"_blank\">" + request.dockerRepository + ":" + request.dockerTagName + "</a>" + " to Bintray...<br/>" +
                "Image will be published in a few moments");
    }
}

