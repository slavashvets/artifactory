package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex;

import org.artifactory.addon.cocoapods.CocoaPodsAddon;
import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Dan Feldman
 */
@JsonTypeName("CocoaPods")
public class PodsIndexCalculator extends BaseIndexCalculator {

    @Override
    public void calculateIndex() throws Exception {
        CocoaPodsAddon podsAddon = addonsManager.addonByType(CocoaPodsAddon.class);
        if (podsAddon != null) {
            podsAddon.reindexAsync(getRepoKey());
        }
    }
}
