package org.artifactory.api.bintray.distribution.rule;

import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;

import static org.artifactory.util.distribution.DistributionConstants.PATH_TOKEN;

/**
 * Denotes an available token for a distribution rule that resolves to the Artifact's path.
 * Can optionally use {@param pathElement} to indicate the value should be populated by the path element in that
 * position after running the path through {@link PathUtils#getPathElements}.
 *
 * @author Dan Feldman
 */
public class DistributionRulePathToken implements DistributionRuleToken {

    private String token = PATH_TOKEN;
    private String value;
    private int pathElement;

    public DistributionRulePathToken(String token) {
        this.token = token;
        this.pathElement = 0;
    }

    public DistributionRulePathToken(String token, int pathElement) {
        this.token = token;
        this.pathElement = pathElement;
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
        if (pathElement > 0) {
            String[] elements = PathUtils.getPathElements(path.getPath());
            if (elements.length < pathElement) {
                throw new Exception("Token " + token + " is unable to resolve a value from path " + path + " as it's" +
                        " trying to match a path location " + pathElement + " that doesn't exist.");
            } else {
                value = elements[pathElement -1];
            }
        } else {
            value = path.getPath();
        }
    }
}
