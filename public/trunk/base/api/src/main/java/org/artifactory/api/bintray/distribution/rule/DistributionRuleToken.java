package org.artifactory.api.bintray.distribution.rule;

import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

/**
 * Denotes an available token for a distribution rule that resolves to a property set on the Artifact.
 *
 * @author Dan Feldman
 */
public interface DistributionRuleToken {

    String getToken();

    String getValue();

    /**
     * Populates the value of this token if it's actual key matches a key in the properties, or the path.
     * the {@param Properties} are used to pass any key-value pair that tokens might use such as actual properties
     * set on an Artifact or layout tokens and their actual value
     */
    void populateValue(RepoPath path, Properties pathProperties) throws Exception;
}
