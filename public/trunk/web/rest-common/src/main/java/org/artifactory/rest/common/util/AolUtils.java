package org.artifactory.rest.common.util;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.DockerRepositoryAction;
import org.artifactory.common.ConstantValues;
import org.artifactory.rest.common.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lior Azar
 */
public class AolUtils {
    private static final Logger log = LoggerFactory.getLogger(AolUtils.class);

    public static void assertNotAol(String functionName) {
        if (ContextHelper.get().beanForType(AddonsManager.class).addonByType(CoreAddons.class).isAol()) {
            log.warn("{} is not supported when running on the cloud", functionName);
            throw new ForbiddenException("Function is not supported when running on the cloud");
        }
    }

    public static void sendDockerRepoEvent(String repoKey, String version, DockerRepositoryAction action) {
        CoreAddons coreAddons = ContextHelper.get().beanForType(AddonsManager.class).addonByType(CoreAddons.class);
        if(coreAddons.isAol() && !ConstantValues.dev.getBoolean()){
               coreAddons.sendDockerRepoEvent(repoKey,version, action);
        }
    }

}
