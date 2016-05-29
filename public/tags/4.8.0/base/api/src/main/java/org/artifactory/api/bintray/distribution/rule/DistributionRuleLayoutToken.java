package org.artifactory.api.bintray.distribution.rule;

import org.apache.commons.lang.StringUtils;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

/**
 * Denotes an available token for a distribution rule that resolves to section of the layout this artifact belongs to
 * (i.e. the layout of the repo storing this artifact)
 *
 * @author Dan Feldman
 */
public class DistributionRuleLayoutToken implements DistributionRuleToken {

    //Token is the same as the layout token
    private String token;
    private String value;

    public DistributionRuleLayoutToken(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void populateValue(RepoPath path, Properties pathProperties) throws Exception {
        value = pathProperties.getFirst(token);
        if (StringUtils.isBlank(value)) {
            throw new Exception("No value was resolved for layout token " + token + " on artifact " + path.toPath());
        }
    }
}
