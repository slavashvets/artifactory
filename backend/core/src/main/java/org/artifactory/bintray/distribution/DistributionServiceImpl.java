package org.artifactory.bintray.distribution;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.jfrog.bintray.client.api.BintrayCallException;
import com.jfrog.bintray.client.api.handle.Bintray;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.bintray.BintrayService;
import org.artifactory.api.bintray.BintrayUploadInfo;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.bintray.distribution.model.DistributionRepoCreationDetails;
import org.artifactory.api.bintray.distribution.resolver.DistributionCoordinatesResolver;
import org.artifactory.api.bintray.distribution.resolver.DistributionRuleFilterType;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleLayoutToken;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleTokens;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.aql.AqlService;
import org.artifactory.bintray.BintrayTokenResponse;
import org.artifactory.bintray.distribution.token.BintrayOAuthTokenRefresher;
import org.artifactory.bintray.distribution.util.DistributionUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.descriptor.repo.distribution.DistributionCoordinates;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.DistributionRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.AlreadyExistsException;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.distribution.DistributionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.artifactory.bintray.distribution.util.DistributionUtils.*;
import static org.artifactory.util.distribution.DistributionConstants.MANIFEST_FILENAME;

/**
 * @author Dan Feldman
 */
@Service
public class DistributionServiceImpl implements DistributionService {
    private static final Logger log = LoggerFactory.getLogger(DistributionServiceImpl.class);

    @Autowired
    private BintrayService bintrayService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AqlService aqlService;

    @Autowired
    private BintrayOAuthTokenRefresher bintrayOAuthTokenRefresher;

    @Autowired
    AuthorizationService authorizationService;

    private static final String OSS_LICENSES_ENDPOINT = "licenses/oss_licenses";
    // private static final String PROPRIETARY_LICENSES_ENDPOINT = "orgs/%s/licenses"; //TODO [by dan]: RTFACT-10217
    private static final String PLAN_ENDPOINT = "orgs/%s/plan";
    private static final String DUMMY_EXISTING_COORDINATES_RULE_NAME = "Existing Coordinates on path";

    @Override
    public DistributionRepoCreationDetails createBintrayAppConfig(String clientId, String secret, String code,
            String scope, String redirectUrl) throws IOException {
        String org = scope.split(":")[1];
        //In order to persist the oauth app before the repo is created we have to randomly generate some ID to be
        //used as the oauth config's key appended to the org. timestamp seems like a good bet.
        String appConfigKey = org + "-" + System.currentTimeMillis();
        BintrayApplicationConfig config = new BintrayApplicationConfig(appConfigKey, clientId, secret, org, scope);
        try (Bintray client = bintrayService.createBlankBintrayClient()) {
            BintrayTokenResponse tokenResponse = getBintrayTokenResponse(code, redirectUrl, client, config);
            log.debug("Adding Bintray Application OAuth config: {}", config.getKey());
            saveNewBintrayAppConfig(config);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + new String(Base64.encodeBase64(tokenResponse.token.getBytes())));
            DistributionRepoCreationDetails details = getRepoCreationDetails(client, org, headers);
            details.oauthAppConfigKey = config.getKey();
            details.oauthToken = tokenResponse.token;
            details.org = org;
            details.clientId = clientId;
            return details;
        }
    }

    private BintrayTokenResponse getBintrayTokenResponse(String code, String redirectUrl, Bintray client,
            BintrayApplicationConfig appConfig) throws IOException {
        BintrayTokenResponse tokenResponse;
        try {
            Map<String, String> requestHeaders = getFormEncodedHeader();
            String basicAuth = Base64.encodeBase64String((appConfig.getClientId() + ":" + appConfig.getSecret()).getBytes());
            requestHeaders.put(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
            log.debug("Executing OAuth token request for org {}", appConfig.getOrg());
            InputStream tokenEntity = getTokenRequestFormParams(appConfig, code, redirectUrl);
            HttpResponse response = client.post("oauth/token", requestHeaders, tokenEntity);
            tokenResponse = JacksonReader.streamAsClass(response.getEntity().getContent(), BintrayTokenResponse.class);
            appConfig.setRefreshToken(tokenResponse.refreshToken);
        } catch (Exception e) {
            //IO can either be problem with streams or failure http return code
            log.error("Error executing get token request: {}", e.getMessage());
            log.debug("Error executing get token request: ", e);
            throw e;
        }
        return tokenResponse;
    }

    //params -> grant_type = authorization_code / code / redirect_uri / client_id / scope / artifactory_hash
    private InputStream getTokenRequestFormParams(BintrayApplicationConfig config, String code,
            String redirectUrl) {
        List<BasicNameValuePair> params = Lists.newArrayList(new BasicNameValuePair("grant_type", "authorization_code"),
                new BasicNameValuePair("code", code),
                new BasicNameValuePair("redirect_uri", redirectUrl),
                new BasicNameValuePair("client_id", config.getClientId()),
                new BasicNameValuePair("scope", config.getScope()),
                new BasicNameValuePair("artifactory_hash", addonsManager.getLicenseKeyHash()));
        return IOUtils.toInputStream(URLEncodedUtils.format(params, "UTF-8"));
    }

    @Override
    public DistributionRepoCreationDetails getRepoCreationDetails(String repoKey) throws IOException {
        DistributionRepo repo = repoService.distributionRepoByKey(repoKey);
        if (repo == null) {
            throw new IOException("No such repo " + repoKey);
        }
        BintrayApplicationConfig appConfig = repo.getDescriptor().getBintrayApplication();
        if (appConfig == null) {
            throw new IOException("Repo " + repoKey + " does not have an OAuth app config, can't retrieve org-specific details.");
        }
        return getRepoCreationDetails(repo.getClient(), appConfig.getOrg(), null);
    }

    @Override
    public BasicStatusHolder distribute(Distribution distribution) {
        if (distribution.isAsync()) {
            ContextHelper.get().beanForType(DistributionService.class).distributeInternal(distribution);
            return new BasicStatusHolder();
        } else {
            return distributeInternal(distribution);
        }
    }

    @Override
    public BasicStatusHolder distributeInternal(Distribution distribution) {
        BasicStatusHolder status = new BasicStatusHolder();
        if (StringUtils.isBlank(distribution.getTargetRepo())) {
            status.error("No distribution repo specified to use for distributing the requested artifact(s).",
                    HttpStatus.SC_BAD_REQUEST, log);
            return status;
        }
        DistributionRepo repo = repoService.distributionRepoByKey(distribution.getTargetRepo());
        if (repo == null) {
            status.error("No such distribution repo " + distribution.getTargetRepo(), HttpStatus.SC_NOT_FOUND, log);
            return status;
        }
        //Distribute permission == deploy to root of dist repo
        if (!authorizationService.canDeploy(repo.getRepoPath("."))) {
            status.error("User does not have the required permissions to distribute to repo " + repo.getKey(),
                    HttpStatus.SC_FORBIDDEN, log);
            return status;
        }
        return repo.distribute(distribution);
    }

    /**
     * Populates {@param details} with this {@param org}'s available licenses - which are Bintray's OSS license list
     * and optionally any other custom license this org has defined (retrieved with a different REST call).
     * Also populates the 'isPremium' field in {@param details}.
     */
    private DistributionRepoCreationDetails getRepoCreationDetails(Bintray client, String org, Map<String, String> headers) throws IOException {
        DistributionRepoCreationDetails details = new DistributionRepoCreationDetails();
        log.debug("Executing plan details request for org {}", org);
        HttpResponse response = client.get(String.format(PLAN_ENDPOINT, org), headers);
        details.isOrgPremium = getIsPremiumFromResponse(response);
        log.debug("Executing OSS Licenses request");
        response = client.get(OSS_LICENSES_ENDPOINT, headers);
        details.orgLicenses.addAll(getLicensesFromResponse(response));
        //TODO [by dan]: find solution for this crap in 4.8.1 RTFACT-10217
        /*if(details.isOrgPremium) {
            log.debug("Executing Proprietary Licenses request for org {}", org);
            response = client.get(String.format(PROPRIETARY_LICENSES_ENDPOINT, org), headers);
            details.orgLicenses.addAll(getLicensesFromResponse(response));
        }*/
        return details;
    }

    @Override
    public String refreshBintrayOAuthAppToken(String repoKey) throws BintrayCallException {
        DistributionRepoDescriptor distRepoDescriptor = repoService.distributionRepoDescriptorByKey(repoKey);
        if (distRepoDescriptor == null || distRepoDescriptor.getBintrayApplication() == null) {
            String err = "Repository " + repoKey + " does not have a Bintray OAuth Application config attached to it.";
            log.debug(err);
            throw new BintrayCallException(HttpStatus.SC_BAD_REQUEST, "", err);
        }
        return bintrayOAuthTokenRefresher.refresh(distRepoDescriptor);
    }

    private void saveNewBintrayAppConfig(BintrayApplicationConfig config) throws IOException,
            AlreadyExistsException {
        MutableCentralConfigDescriptor configDescriptor = configService.getMutableDescriptor();
        try {
            configDescriptor.addBintrayApplication(config);
        } catch (AlreadyExistsException aee) {
            String newAppConfigKey = config.getKey() + "-new";
            log.warn(aee.getMessage() + " trying to create a new one: '{}'", newAppConfigKey);
            config.setKey(newAppConfigKey);
            try {
                configDescriptor.addBintrayApplication(config);
            } catch (AlreadyExistsException aee2) {
                log.error(aee2.getMessage() + " To avoid further confusion in your config the save will fail to give" +
                        " you a chance to sort it out.");
                throw aee2;
            }
        }
        configService.saveEditedDescriptorAndReload(configDescriptor);
    }

    public Multimap<BintrayUploadInfo, DistributionCoordinatesResolver> coordinatesForPaths(List<RepoPath> paths,
            List<DistributionRule> repoRules, DistributionRepoDescriptor descriptor, BasicStatusHolder status) {
        paths = adjustSpecialPaths(paths, status);
        Map<RepoPath, Properties> pathProperties = getPathProperties(paths, aqlService);
        if (pathProperties.isEmpty()) {
            //If any paths were found they will have entries in the map with empty Properties as values
            status.error("No Artifacts found for any of the paths you specified, nothing to distribute.",
                    HttpStatus.SC_NOT_FOUND, log);
            return HashMultimap.create();
        }
        String productName = descriptor.getProductName();
        DistributionUtils.insertProductNameDummyProp(productName, pathProperties);
        Map<String, List<String>> layoutTokensByRepoKey = addLayoutTokens(pathProperties, status);
        log.debug("Populating coordinate tokens with artifact coordinates and layout data.");
        List<DistributionCoordinatesResolver> resolvers =
                getCoordinatesForPaths(pathProperties, repoRules, productName, status, layoutTokensByRepoKey).stream()
                        .map(coordinate -> coordinate.resolve(status))
                        .filter(Objects::nonNull)
                        .map(coordinate -> coordinate.populateUploadInfo(descriptor))
                        .collect(Collectors.toList());

        return mergeVersions(resolvers);
    }

    /**
     * Merges the {@link BintrayUploadInfo} of resolvers that point to the same repo/package/version from
     * {@param resolvers} and aggregates all attributes into the same version so that we only create it once when
     * distributing.
     */
    private Multimap<BintrayUploadInfo, DistributionCoordinatesResolver> mergeVersions(
            List<DistributionCoordinatesResolver> resolvers) {
        Multimap<BintrayUploadInfo, DistributionCoordinatesResolver> mergedResolvers = HashMultimap.create();

        for (DistributionCoordinatesResolver resolver : resolvers) {
            if (!mergedResolvers.containsKey(resolver.uploadInfo)) {
                List<DistributionCoordinatesResolver> sameCoordinates = getResolversWithSameUploadInfo(resolvers, resolver);
                BintrayUploadInfo mergedUploadInfo = getMergedUploadInfo(sameCoordinates);
                if (mergedUploadInfo != null) {
                    mergedResolvers.putAll(mergedUploadInfo, sameCoordinates);
                } else {
                    //If something went wrong fall back to upload info per coordinate
                    sameCoordinates.forEach(coordinate -> mergedResolvers.put(coordinate.uploadInfo, coordinate));
                }
            }
        }
        //If something went wrong fall back to upload info per coordinate
        if (mergedResolvers.isEmpty()) {
            resolvers.forEach(coordinate -> mergedResolvers.put(coordinate.uploadInfo, coordinate));
        }
        return mergedResolvers;
    }

    /**
     * Adds layout tokens and values as {@param pathProperties} according to each path
     */
    private Map<String, List<String>> addLayoutTokens(Map<RepoPath, Properties> pathProperties, BasicStatusHolder status) {
        Map<String, List<String>> layoutTokensByRepoKey = Maps.newHashMap();
        for (Map.Entry<RepoPath, Properties> entry : pathProperties.entrySet()) {
            RepoPath path = entry.getKey();
            String repoKey = path.getRepoKey();
            List<String> layoutTokens;
            if (layoutTokensByRepoKey.containsKey(repoKey)) {
                layoutTokens = layoutTokensByRepoKey.get(repoKey);
            } else {
                LocalRepoDescriptor descriptor = repoService.localOrCachedRepoDescriptorByKey(repoKey);
                if (descriptor == null) {
                    status.warn("No such repo " + repoKey + " , Distribution coordinates will not be resolved for " +
                            "artifact " + path.toPath() + " .", HttpStatus.SC_NOT_FOUND, log);
                    continue;
                }
                layoutTokens = RepoLayoutUtils.getLayoutTokens(descriptor.getRepoLayout());
                log.trace("Adding layout tokens {} retrieved from repo {} layout {}.", layoutTokens, repoKey,
                        descriptor.getRepoLayout().getName());
                layoutTokensByRepoKey.put(repoKey, layoutTokens);
            }
            try {
                ModuleInfo moduleInfo = repoService.getItemModuleInfo(path);
                for (String token : layoutTokens) {
                    String strippedToken = DistributionUtils.stripTokenBrackets(token);
                    String value = ModuleInfoUtils.getTokenValue(moduleInfo, strippedToken);
                    log.trace("Layout token matched value {} for layout token {} on path {}", value, token, path.toPath());
                    pathProperties.get(path).put(DistributionConstants.wrapToken(strippedToken), value);
                }
            } catch (Exception e) {
                status.warn("Failed to get layout information for artifact '" + path.toPath() + "', layout tokens will "
                        + "not be resolved for it.", HttpStatus.SC_BAD_REQUEST, log);
            }
        }
        return layoutTokensByRepoKey;
    }

    /**
     * Constructs A {@link DistributionCoordinatesResolver} for each path given in {@param pathProperties} based on
     * the path's type and if it matched any {@link DistributionRule} from the given list {@param rules}
     */
    private List<DistributionCoordinatesResolver> getCoordinatesForPaths(Map<RepoPath, Properties> pathProperties,
                                                                         List<DistributionRule> rules, @Nullable String productName,
                                                                         BasicStatusHolder status, Map<String, List<String>> layoutTokensByRepoKey) {
        List<DistributionCoordinatesResolver> coordinates = Lists.newArrayList();
        Map<String, RepoType> repoTypes = Maps.newHashMap();
        for (Map.Entry<RepoPath, Properties> pathProps : pathProperties.entrySet()) {
            RepoPath path = pathProps.getKey();
            if (!repoExists(repoTypes, path, status)) {
                //Repo doesn't exist, skip it.
                continue;
            }
            Properties properties = pathProps.getValue();
            DistributionCoordinatesResolver resolver =
                    getResolverByPathAndProperties(rules, repoTypes.get(path.getRepoKey()), path, properties, status);
            if (resolver != null) {
                //Add product name token if this repo distributes a product, only for new rules, not existing coordinates.
                if (StringUtils.isNotBlank(productName) && !DUMMY_EXISTING_COORDINATES_RULE_NAME.equals(resolver.ruleName)) {
                    resolver.tokens.add(DistributionRuleTokens.getProductNameToken());
                }
                //Add layout tokens according to the layout defined for the repo
                layoutTokensByRepoKey.get(path.getRepoKey()).stream().forEach(layoutToken -> {
                    String strippedToken = DistributionUtils.stripTokenBrackets(layoutToken);
                    String wrappedToken = DistributionConstants.wrapToken(strippedToken);
                    resolver.tokens.add(new DistributionRuleLayoutToken(wrappedToken));
                });
                coordinates.add(resolver);
            }
        }
        return coordinates;
    }

    private DistributionCoordinatesResolver getResolverByPathAndProperties(List<DistributionRule> rules, RepoType type,
            RepoPath path, Properties properties, BasicStatusHolder status) {
        DistributionCoordinatesResolver resolver;
        if (DistributionCoordinatesResolver.pathHasDistributionCoordinates(properties)) {
            log.debug("Found existing Bintray Properties on path {}, returning dummy rule which will populate " +
                    "coordinates with values later on.", path);
            DistributionCoordinates dummyCoordinates = new DistributionCoordinates("", "", "", "");
            DistributionRule dummyRule = new DistributionRule(DUMMY_EXISTING_COORDINATES_RULE_NAME, type, "", "", dummyCoordinates);
            resolver = new DistributionCoordinatesResolver(dummyRule, path, properties);
        } else {
            resolver = matchRuleToArtifact(rules, type, properties, path, status);
        }
        return resolver;
    }

    private DistributionCoordinatesResolver matchRuleToArtifact(List<DistributionRule> rules, RepoType repoType,
            Properties pathProps, RepoPath path, BasicStatusHolder status) {
        DistributionCoordinatesResolver resolver = null;
        RepoType artifactType = getArtifactType(pathProps, repoType, path, status);
        if (artifactType != null) {
            resolver = matchPathToRule(rules, path, artifactType, pathProps, status);
        } else {
            status.error("Can't match any rule to artifact '" + path.toPath() + "'.", HttpStatus.SC_BAD_REQUEST, log);
        }
        return resolver;
    }

    /**
     * Matches the first rule from the ordered rules list {@param rules} to the give {@param path} based on it's
     * {@param artifactType} and the rule's {@link DistributionRule#pathFilter} if specified.
     */
    private DistributionCoordinatesResolver matchPathToRule(List<DistributionRule> rules, RepoPath path,
            RepoType artifactType, Properties pathProps, BasicStatusHolder status) {
        for (DistributionRule rule : rules) {
            //First rule that matches, either of same type or generic
            if (rule.getType().equals(artifactType) || rule.getType().equals(RepoType.Generic)) {
                DistributionCoordinatesResolver resolver = new DistributionCoordinatesResolver(rule, path, pathProps);
                boolean repoMatches = addCaptureGroupsToRuleResolverIfMatches(rule, DistributionRuleFilterType.repo, rule.getRepoFilter(), path.getRepoKey(), resolver);
                if (!repoMatches) {
                    continue;
                }
                boolean pathMatches = addCaptureGroupsToRuleResolverIfMatches(rule, DistributionRuleFilterType.path, rule.getPathFilter(), path.getPath(), resolver);
                if (pathMatches) {
                    return resolver;
                }
            }
        }
        status.error("Failed to match any rule for artifact " + path.toPath() + " with type " + artifactType + ". It " +
                "will not be distributed.", HttpStatus.SC_BAD_REQUEST, log);
        return null;
    }

    /**
     * Adds capture groups to the resolver if there is a filter to use and the given text matches it.
     *
     * @return <code>true</code> if the given text matches the filter, or <code>false</code> otherwise.
     * Blank filter always returns <code>true</code>.
     */
    private boolean addCaptureGroupsToRuleResolverIfMatches(DistributionRule rule, DistributionRuleFilterType filterType,
            String filterRegex, String textToMatch, DistributionCoordinatesResolver resolver) {
        if (StringUtils.isNotBlank(filterRegex)) {
            Pattern filterPattern = Pattern.compile(filterRegex);
            Matcher filterMatcher = createTimingOutMatcher(textToMatch, filterPattern);
            if (filterMatcher.matches()) {
                addCaptureGroupsToRuleResolver(filterType, filterMatcher, resolver);
            } else {
                log.debug("Failed to match rule {} with {} filter {} to value {}", rule.getName(),
                        filterType.getQualifier(), filterRegex, textToMatch);
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies folders in Docker repos that were given in {@param paths} and adjust the path to point to the
     * manifest file.
     */
    private List<RepoPath> adjustSpecialPaths(List<RepoPath> paths, BasicStatusHolder status) {
        return paths.stream()
                .map(path -> adjustDirToManifest(path, status))
                .collect(Collectors.toList());
    }

    /**
     * @return the path to the manifest.json file if this {@param path} points to a docker tag directory.
     */
    private RepoPath adjustDirToManifest(RepoPath path, BasicStatusHolder status) {
        RepoPath adjustedPath = path;
        if (!isDockerRepo(path)) {
            return adjustedPath;
        }
        String fullPath = path.toPath();
        try {
            if (repoService.getItemInfo(path).isFolder()) {
                status.status("Path " + fullPath + " is a folder in a Docker repo, checking for tag manifests.", log);
                List<ItemInfo> children = repoService.getChildren(path);
                List<ItemInfo> manifestChildren = children.stream()
                        .filter(item -> !item.isFolder())
                        .filter(item -> MANIFEST_FILENAME.equals(item.getName()))
                        .collect(Collectors.toList());
                if (manifestChildren.size() < 1) {
                    status.status("Directory " + fullPath + " has no manifest.json files under it, skipping.", log);
                } else if (manifestChildren.size() > 1) {
                    status.status("Directory " + fullPath + " has many manifest.json files under it, skipping.", log);
                } else {
                    adjustedPath = manifestChildren.get(0).getRepoPath();
                    log.debug("Adjusting path {} to its underlying manifest.json in {}", fullPath, adjustedPath);
                    status.status("Found manifest under " + fullPath, log);
                }
            }
        } catch (ItemNotFoundRuntimeException nfe) {
            status.error("Path " + fullPath + " not found.", HttpStatus.SC_NOT_FOUND, log);
        }
        return adjustedPath;
    }

    /**
     * @return true if {@param path}'s repo exists. Also populates the repo->type cache with that repo's type.
     */
    private boolean repoExists(Map<String, RepoType> repoTypes, RepoPath path, BasicStatusHolder status) {
        String repoKey = path.getRepoKey();
        if (repoTypes.get(repoKey) == null) {
            RepoBaseDescriptor descriptor = repoService.localOrCachedRepoDescriptorByKey(repoKey);
            if (descriptor == null) {
                status.error("Received path " + path.toPath() + " to distribute, but no such repo " + repoKey + ".", log);
                return false;
            }
            repoTypes.putIfAbsent(repoKey, descriptor.getType());
        }
        return true;
    }

    /**
     * @return true if {@param path} is in a Docker repo.
     */
    private boolean isDockerRepo(RepoPath path) {
        RepoDescriptor descriptor = repoService.repoDescriptorByKey(path.getRepoKey());
        return descriptor != null && RepoType.Docker.equals(descriptor.getType());
    }
}
