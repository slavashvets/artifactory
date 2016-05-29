package org.artifactory.rest.common.util;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.build.BuildInfoUtils;
import org.artifactory.build.BuildRun;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.DoesNotExistException;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
public class BuildResourceHelper {
    private static final Logger log = LoggerFactory.getLogger(BuildResourceHelper.class);

    public static Build getBuild(String buildName, String buildNumber, BasicStatusHolder status) {
        Build build = null;
        try {
            BuildRun buildRun = BuildResourceHelper.validateParamsAndGetBuildInfo(buildName, buildNumber, null);
            build = ContextHelper.get().beanForType(BuildService.class).getBuild(buildRun);
        } catch (Exception e) {
            status.error("Can't get build: " + buildName + ":" + buildNumber + " - " + e.getMessage(), log);
        }
        return build;
    }

    /**
     * Executes a build artifact search that excludes results from all distribution repos to avoid the search
     * finding artifacts that were already distributed which will cause the distribution to deploy them in their already
     * existing paths in Bintray (because they already have the bintray coordinates properties)
     */
    public static void populateBuildPaths(Build build, Distribution distribution, BasicStatusHolder status) {
        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        List<String> buildArtifactPaths =
                buildService.collectBuildArtifacts(build, distribution.getSourceRepos(), getAllDistRepoKeys(), status)
                .stream()
                .map(FileInfo::getRepoPath)
                .map(RepoPath::toPath)
                .collect(Collectors.toList());
        distribution.setPackagesRepoPaths(buildArtifactPaths);
    }

    private static List<String> getAllDistRepoKeys() {
        return ContextHelper.get().beanForType(RepositoryService.class).getDistributionRepoDescriptors().stream()
                .map(RepoDescriptor::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Validates the parameters of the move\copy request and returns the basic build info object if found
     *
     * @param buildName   Name of build to target
     * @param buildNumber Number of build to target
     * @param started     Start date of build to target (can be null)
     * @return Basic info of build to target
     */
    public static BuildRun validateParamsAndGetBuildInfo(String buildName, String buildNumber, String started)
            throws ParseException {

        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name cannot be blank.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number cannot be blank.");
        }

        BuildRun toReturn = getRequestedBuildInfo(buildName, buildNumber, started);

        if (toReturn == null) {
            throw new DoesNotExistException("Cannot find build by the name '" + buildName + "' and the number '" +
                    buildNumber + "' which started on " + started + ".");
        }

        return toReturn;
    }

    /**
     * Returns the basic info object of the build to target
     *
     * @param buildName   Name of build to target
     * @param buildNumber Number of build to target
     * @param started     Start date of build to target (can be null)
     * @return Basic info of build to target
     */
    public static BuildRun getRequestedBuildInfo(String buildName, String buildNumber, String started) {
        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        Set<BuildRun> buildRunSet = buildService.searchBuildsByNameAndNumber(buildName, buildNumber);
        if (buildRunSet.isEmpty()) {
            throw new DoesNotExistException("Cannot find builds by the name '" + buildName + "' and the number '" +
                    buildNumber + "'.");
        }
        BuildRun toReturn = null;

        if (StringUtils.isBlank(started)) {
            for (BuildRun buildRun : buildRunSet) {
                if ((toReturn == null) || toReturn.getStartedDate().before(buildRun.getStartedDate())) {
                    toReturn = buildRun;
                }
            }
        } else {
            Date requestedStartDate = new Date(BuildInfoUtils.parseBuildTime(started));
            for (BuildRun buildRun : buildRunSet) {
                if (buildRun.getStartedDate().equals(requestedStartDate)) {
                    toReturn = buildRun;
                    break;
                }
            }
        }
        return toReturn;
    }
}
