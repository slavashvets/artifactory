package org.artifactory.ui.rest.service.builds.bintray;

import org.apache.http.HttpStatus;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.util.BuildResourceHelper;
import org.artifactory.ui.rest.model.builds.BuildCoordinate;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.governance.AbstractBuildService;
import org.artifactory.ui.rest.service.utils.distribution.DistributionUIResponseUtils;
import org.artifactory.ui.utils.DateUtils;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.ParseException;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DistributeBuildService extends AbstractBuildService {
    private static final Logger log = LoggerFactory.getLogger(DistributeBuildService.class);

    @Autowired
    DistributionService distService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        // boolean isDryRun = Boolean.parseBoolean(request.getQueryParamByKey("dryRun")); //TODO [by dan]: handle dry run
        boolean async = Boolean.parseBoolean(request.getQueryParamByKey("async"));
        String targetRepo = request.getQueryParamByKey("targetRepo");
        boolean overrideExistingFiles = Boolean.parseBoolean(request.getQueryParamByKey("overrideExistingFiles"));
        // String sourceRepo = request.getQueryParamByKey("limitSourceRepo"); //TODO [by dan]:  phase 2
        BuildCoordinate buildCoordinate = (BuildCoordinate) request.getImodel();
        String buildName = buildCoordinate.getBuildName();
        String buildNumber = buildCoordinate.getBuildNumber();
        String buildStarted;
        try {
            buildStarted = DateUtils.formatBuildDate(buildCoordinate.getDate());
        } catch (ParseException pex) {
            response.error("The specified build start time is invalid");
            response.responseCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        Build build = getBuild(buildName, buildNumber, buildStarted, response);
        if (response.isFailed()) {
            return;
        }
        BasicStatusHolder status = new BasicStatusHolder();
        Distribution distribution = new Distribution();
        distribution.setTargetRepo(targetRepo);
        // distribution.setSourceRepo(sourceRepo);
        distribution.setAsync(async);
        // distribution.setDryRun(isDryRun); //TODO [by dan]: handle dry run
        distribution.setOverrideExistingFiles(overrideExistingFiles);
        BuildResourceHelper.populateBuildPaths(build, distribution, status);
        if (!status.isError()) {
            status = distService.distribute(distribution);
        }
        response.iModel(DistributionUIResponseUtils.createResponseEntity(buildName + ":" + buildNumber, targetRepo, status));
    }
}
