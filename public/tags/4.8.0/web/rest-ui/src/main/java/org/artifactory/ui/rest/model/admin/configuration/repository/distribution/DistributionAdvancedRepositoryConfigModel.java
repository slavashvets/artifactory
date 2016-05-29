package org.artifactory.ui.rest.model.admin.configuration.repository.distribution;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.local.LocalAdvancedRepositoryConfigModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_GPG_SIGN;

/**
 * @author Dan Feldman
 */
public class DistributionAdvancedRepositoryConfigModel extends LocalAdvancedRepositoryConfigModel {

    private List<DistributionRule> distributionRules = Lists.newArrayList();
    private String proxy;
    private boolean gpgSign = DEFAULT_GPG_SIGN;
    private String gpgPassPhrase;
    private Set<String> whiteListedProperties = new HashSet<>();

    public List<DistributionRule> getDistributionRules() {
        return distributionRules;
    }

    public void setDistributionRules(List<DistributionRule> distributionRules) {
        this.distributionRules = distributionRules;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public Set<String> getWhiteListedProperties() {
        return whiteListedProperties;
    }

    public void setWhiteListedProperties(Set<String> whiteListedProperties) {
        this.whiteListedProperties = whiteListedProperties;
    }

    public boolean isGpgSign() {
        return gpgSign;
    }

    public void setGpgSign(boolean gpgSign) {
        this.gpgSign = gpgSign;
    }

    public String getGpgPassPhrase() {
        return gpgPassPhrase;
    }

    public void setGpgPassPhrase(String gpgPassPhrase) {
        this.gpgPassPhrase = gpgPassPhrase;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
