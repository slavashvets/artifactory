package org.artifactory.post.providers.features;

import org.artifactory.api.callhome.FeatureGroup;

/**
 * This is an interface that should be implemented by any class that represent a feature
 * (e.g. repositories feature, security feature etc..)
 * <p>
 *
 * @author Shay Bagants
 */
public interface CallHomeFeature {

    FeatureGroup getFeature();
}
