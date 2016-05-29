package org.artifactory.api.bintray.distribution.rule;

import org.apache.commons.collections.CollectionUtils;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Set;

/**
 * Denotes an available token for a distribution rule that resolves to a property set on the Artifact.
 *
 * @author Dan Feldman
 */
public class DistributionRulePropertyToken implements DistributionRuleToken {

    @JsonIgnore
    private String propertyKey;
    private String token;
    private String value;

    public DistributionRulePropertyToken(String token, String key) {
        this.token = token;
        this.propertyKey = key;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getValue() {
        return value;
    }

    @JsonIgnore
    public String getPropertyKey() {
        return propertyKey;
    }

    @Override
    public void populateValue(RepoPath path, Properties pathProperties) throws Exception {
        Set<String> values = pathProperties.get(propertyKey);
        if (CollectionUtils.isEmpty(values)) {
            throw new Exception("No value was resolved for property token " + token + " on artifact " + path.toPath()
            + ", which is resolved from property: " + propertyKey);
        }
        value = PathUtils.collectionToDelimitedString(values);
    }
}
