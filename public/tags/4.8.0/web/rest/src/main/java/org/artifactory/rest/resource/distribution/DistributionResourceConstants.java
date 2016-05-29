package org.artifactory.rest.resource.distribution;

import org.artifactory.api.rest.constant.RestConstants;

/**
 * @author Dan Feldman
 */
public interface DistributionResourceConstants {

    String PATH_ROOT = "distribute";
    String MT_DISTRIBUTION = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".Distribution+json";
}
