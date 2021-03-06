package org.artifactory.ui.rest.service.artifacts.search.searchresults;

import org.artifactory.api.search.ItemSearchResult;
import org.artifactory.api.search.SavedSearchResults;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.ui.rest.model.artifacts.search.BaseSearchResult;
import org.artifactory.ui.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SubtractSearchResultsService extends BaseSearchResultService {
    private static final Logger log = LoggerFactory.getLogger(RemoveSearchResultsService.class);


    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String resultName = request.getQueryParamByKey("name");
        boolean useVersionLevel = Boolean.valueOf(request.getQueryParamByKey("useVersion"));
        List<BaseSearchResult> baseSearchResults = (List<BaseSearchResult>) request.getModels();
        SavedSearchResults savedSearchResults = RequestUtils.getResultsFromRequest(resultName,
                request.getServletRequest());
        if (savedSearchResults == null) {
            response.error(format("No existing search result named '%s' was found.", resultName));
            return;
        } else {
            subtractResults(request, response, resultName, baseSearchResults, savedSearchResults, useVersionLevel);
        }
    }

    /**
     * subtract result with saved result
     *
     * @param request            - http servlet request
     * @param response           - http servlet response
     * @param resultName         - result name
     * @param baseSearchResults  - results to subtract
     * @param savedSearchResults - saved results
     */
    private void subtractResults(ArtifactoryRestRequest request, RestResponse response, String resultName,
            List<BaseSearchResult> baseSearchResults, SavedSearchResults savedSearchResults, boolean useVersionLevel) {
        List<ItemSearchResult> results = new ArrayList<>();
        SavedSearchResults resultsToSubtract = getSavedSearchResults(resultName, results, baseSearchResults,
                useVersionLevel);
        if (resultsToSubtract.isEmpty()) {
            response.warn("No search results were subtracted - No published artifacts or dependencies found.");
        } else {
            savedSearchResults.subtract(resultsToSubtract);
            if (!savedSearchResults.isEmpty()) {
                response.info("Search results successfully subtracted");
            } else {
                try {
                    RequestUtils.removeResultsToRequest(resultName, request.getServletRequest());
                } catch (Exception e) {
                    log.error(e.toString());
                }
                String message = new StringBuilder().append("Successfully subtracted search results from stash").toString();                response.info(message);
            }
        }
    }

}
