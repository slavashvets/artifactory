package org.artifactory.ui.rest.service.utils.distribution;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.common.StatusEntryLevel;
import org.artifactory.rest.common.model.FeedbackMsg;
import org.artifactory.ui.rest.model.artifacts.distribute.DistributeArtifactStatusModel;

/**
 * @author Dan Feldman
 */
public class DistributionUIResponseUtils {

    /**
     * @param distributedItem - build or artifact path that is distributed.
     */
    public static DistributeArtifactStatusModel createResponseEntity(String distributedItem, String targetRepo, BasicStatusHolder status) {
        DistributeArtifactStatusModel model = new DistributeArtifactStatusModel();
        FeedbackMsg feedback = new FeedbackMsg();
        if (status == null) {
            feedback.setError("No status received from distribution operation for path " + distributedItem + " using " +
                    "distribution repo " + targetRepo + ". Check the logs for more information.");
        } else {
            //This signifies async - the service returns an empty status holder after spawning thread.
            if (status.getEntries().size() == 0) {
                feedback.setInfo("Distribution of " + distributedItem + " to target repository " + targetRepo + " scheduled successfully.");
            }
            if (status.hasErrors()) {
                feedback.setError("Distribution of " + distributedItem + " to target repository " + targetRepo + " ended with " +
                        status.getErrors().size() + (status.getErrors().size() == 1 ? " error." : " errors."));
            }
            long successfullyDeployed = status.getEntries(StatusEntryLevel.INFO).stream()
                    .filter(entry -> entry.getMessage().startsWith("Successfully deployed")).count();
            if (successfullyDeployed > 0) {
                feedback.setInfo("Distribution of " + distributedItem + " to target repository " + targetRepo + " successfully deployed " +
                        successfullyDeployed + (successfullyDeployed == 1 ? " artifact." : "artifacts."));
            }
            if (status.hasWarnings()) {
                feedback.setWarn("Distribution of " + distributedItem + " to target repository " + targetRepo + " ended with "
                        + status.getWarnings().size() + (status.getWarnings().size() == 1 ? " warning." : " warnings."));
            }
        }
        model.setFeedbackMsg(feedback);
        return model;
    }
}
