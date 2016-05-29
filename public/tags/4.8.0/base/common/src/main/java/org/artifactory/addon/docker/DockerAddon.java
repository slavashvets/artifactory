/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.addon.docker;

import org.artifactory.addon.Addon;
import org.artifactory.addon.docker.rest.DockerTokenCacheKey;
import org.artifactory.api.bintray.docker.BintrayDockerPushRequest;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author Shay Yaakov
 */
public interface DockerAddon extends Addon {

    /**
     * Pushes the image:tag from {@param repoKey} given in {@param request} to Bintray, can optionally use a
     * distribution repository's client instead of a client that's created for the current user
     * if {@param distRepoKey} is specified.
     */
    void pushTagToBintray(String repoKey, BintrayDockerPushRequest request, @Nullable String distRepoKey);

    DockerV2InfoModel getDockerV2Model(RepoPath manifestPath) throws IOException;

    /**
     * Retrieves a new auth token for the {@param tokenCacheKey} passed to it.
     */
    String fetchDockerAuthToken(DockerTokenCacheKey tokenCacheKey);
}