package org.artifactory.api.bintray.distribution.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds model info that can only be retrieved with valid auth information that users of this model
 * supply to the {@link org.artifactory.api.bintray.distribution.DistributionService} when calling relevant methods
 *
 * @author Dan Feldman
 */
public class DistributionRepoCreationDetails {

    public String oauthAppConfigKey;
    public String oauthToken;
    public List<String> orgLicenses = new ArrayList<>();
    public boolean isOrgPremium = false;
    public String org;
    public String clientId;

}
