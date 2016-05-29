/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.rest.resource.distribution;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.rest.constant.BintrayRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.util.BintrayRestHelper;
import org.artifactory.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.artifactory.rest.common.util.BintrayRestHelper.createAggregatedResponse;
import static org.artifactory.rest.resource.distribution.DistributionResourceConstants.MT_DISTRIBUTION;

/**
 * This endpoint provides all distribution actions separated by package type being pushed.
 * Each endpoint relies on the specific package metadata and runs only the relevant rules in the repo.
 *
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path("distribute")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class DistributionResource {
    private static final Logger log = LoggerFactory.getLogger(DistributionResource.class);

    @Autowired
    private DistributionService distributionService;

    /**
     * Distributes a set of paths to Bintray using the params specified in {@param distribution} and the dist repo's
     * rule set. if {@param gpgPassphrase} was passed it will be used as an override and the each created version will
     * be signed with it.
     *
     * @return Status of the operation
     */
    @POST
    @Consumes({MT_DISTRIBUTION, MediaType.APPLICATION_JSON})
    @Produces({BintrayRestConstants.MT_BINTRAY_PUSH_RESPONSE, MediaType.APPLICATION_JSON})
    public Response distribute(@QueryParam("gpgPassphrase") @Nullable String gpgPassphrase, Distribution distribution) {
        BasicStatusHolder status = new BasicStatusHolder();
        validateParams(distribution, status);
        if (status.isError()) {
            return createAggregatedResponse(status, "the requested artifacts", false);
        }
        if (StringUtils.isNotEmpty(gpgPassphrase)) {
            distribution.setGpgPassphrase(gpgPassphrase);
        }
        status.merge(distributionService.distribute(distribution));
        return createAggregatedResponse(status, "the requested artifacts", distribution.isAsync());
    }

    private void validateParams(Distribution distribution, BasicStatusHolder status) {
        if (!BintrayRestHelper.isPushToBintrayAllowed(status, distribution.getTargetRepo())) {
            throw new AuthorizationRestException(status.getLastError().getMessage());
        } else if (CollectionUtils.notNullOrEmpty(distribution.getSourceRepos())) {
            status.error("Source repositories filtration is only available for build distribution.", SC_BAD_REQUEST, log);
        }
    }
}
