package org.artifactory.ui.rest.model.admin.configuration.repository.distribution.rule;

import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleToken;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;

/**
 * @author Dan Feldman
 */
public class DistributionRuleTokenModel implements DistributionRuleToken, RestModel {

    private String token;
    private String value;

    public DistributionRuleTokenModel() {

    }

    public DistributionRuleTokenModel(DistributionRuleToken distToken) {
        this.token = distToken.getToken();
        this.value = distToken.getValue();
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
    public void populateValue(RepoPath path, Properties pathProperties) {
        //No need to set value on the model itself.
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
