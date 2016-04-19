package org.artifactory.rest.services;

import org.artifactory.rest.common.service.trash.EmptyTrashService;
import org.artifactory.rest.common.service.trash.RestoreArtifactService;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Shay Yaakov
 */
public abstract class RepoServiceFactory {

    @Lookup
    public abstract EmptyTrashService emptyTrashService();

    @Lookup
    public abstract RestoreArtifactService restoreArtifactService();
}
