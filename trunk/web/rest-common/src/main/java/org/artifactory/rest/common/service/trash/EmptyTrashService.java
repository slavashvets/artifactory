package org.artifactory.rest.common.service.trash;

import org.apache.http.HttpStatus;
import org.artifactory.common.StatusHolder;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EmptyTrashService implements RestService {

    @Autowired
    TrashService trashService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        StatusHolder statusHolder = trashService.empty();
        if (request.isUiRestCall()) {
            uiResponse(statusHolder, response);
        } else {
            apiResponse(statusHolder, response);
        }
    }

    private void uiResponse(StatusHolder statusHolder, RestResponse response) {
        if (statusHolder.isError()) {
            response.error(statusHolder.getLastError().getMessage());
        } else {
            response.info("Successfully deleted all trashcan items");
        }
    }

    private void apiResponse(StatusHolder statusHolder, RestResponse response) {
        if (statusHolder.isError()) {
            throw new BadRequestException(statusHolder.getLastError().getMessage());
        } else {
            response.iModel("Successfully deleted all trashcan items");
            response.responseCode(HttpStatus.SC_ACCEPTED);
        }
    }
}
