package org.artifactory.rest.common.service.trash;

import org.apache.http.HttpStatus;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.common.model.trash.RestoreArtifact;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Restore an artifact from the trashcan to it's original repository (or to the given destination)
 *
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestoreArtifactService implements RestService {

    @Autowired
    TrashService trashService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        RestoreArtifact restoreArtifact = (RestoreArtifact) request.getImodel();
        String repoKey = restoreArtifact.getRepoKey();
        String path = restoreArtifact.getPath();
        MoveMultiStatusHolder status = trashService.restore(InternalRepoPathFactory.create(repoKey, path),
                restoreArtifact.getTargetRepoKey(), restoreArtifact.getTargetPath());
        if (request.isUiRestCall()) {
            uiResponse(status, response);
        } else {
            apiResponse(status, response);
        }
    }

    private void uiResponse(MoveMultiStatusHolder status, RestResponse response) {
        if (status.isError()) {
            response.error(status.getLastError().getMessage());
        } else if (status.hasWarnings()) {
            response.warn(status.getLastWarning().getMessage());
        } else {
            response.info("Successfully restored trash items");
        }
    }

    private void apiResponse(MoveMultiStatusHolder statusHolder, RestResponse response) {
        if (statusHolder.isError()) {
            throw new BadRequestException(statusHolder.getLastError().getMessage());
        } else if (statusHolder.hasWarnings()) {
            throw new BadRequestException(statusHolder.getLastWarning().getMessage());
        } else {
            response.iModel("Successfully restored trash items");
            response.responseCode(HttpStatus.SC_ACCEPTED);
        }
    }
}
