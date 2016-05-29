package org.artifactory.bintray.distribution.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.jfrog.bintray.client.api.BintrayCallException;
import com.jfrog.bintray.client.api.handle.RepositoryHandle;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.artifactory.api.bintray.BintrayUploadInfo;
import org.artifactory.api.bintray.distribution.resolver.DistributionCoordinatesResolver;
import org.artifactory.api.bintray.distribution.resolver.DistributionRuleFilterType;
import org.artifactory.api.bintray.distribution.rule.DistributionRulePropertyToken;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleToken;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.util.AqlSearchablePath;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.AutoTimeoutRegexCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.artifactory.util.distribution.DistributionConstants.ARTIFACT_TYPE_OVERRIDE_PROP;
import static org.artifactory.util.distribution.DistributionConstants.PRODUCT_NAME_DUMMY_PROP;

/**
 * @author Dan Feldman
 */
public class DistributionUtils {
    private static final Logger log = LoggerFactory.getLogger(DistributionUtils.class);

    /**
     * Adds application/x-www-form-urlencoded header required by the spec
     * https://tools.ietf.org/html/rfc6749#section-4.1.3
     */
    public static Map<String, String> getFormEncodedHeader() {
        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        return headers;
    }

    /**
     * @return a mapping of {@param paths} and the properties set on them, returns an empty (not null)
     * {@link Properties} object for paths that had no properties
     */
    public static Map<RepoPath, Properties> getPathProperties(Collection<RepoPath> paths, AqlService aqlService) {
        AqlBase pathProperties = getAqlSearchClauseForPaths(paths);
        log.trace("Built aql query {} to search path properties on paths: {}", pathProperties.toNative(0), paths.toString());
        AqlEagerResult<AqlBaseFullRowImpl> results = aqlService.executeQueryEager(pathProperties);
        HashMultimap<RepoPath, AqlBaseFullRowImpl> pathRows = getPathRows(results);
        return pathsWithPropsFromAql(pathRows);
    }

    private static AqlBase getAqlSearchClauseForPaths(Collection<RepoPath> paths) {
        AqlBase.OrClause aqlPaths = AqlUtils.getSearchClauseForPaths(paths
                .stream()
                .map(AqlSearchablePath::new)
                .collect(Collectors.toList()));
        return AqlApiItem.create().filter(aqlPaths).include(AqlApiItem.property().key(), AqlApiItem.property().value());
    }

    private static HashMultimap<RepoPath, AqlBaseFullRowImpl> getPathRows(AqlEagerResult<AqlBaseFullRowImpl> results) {
        return AqlUtils.aggregateResultsByPath(results.getResults(), ContextHelper.get().getAuthorizationService());
    }

    /**
     * @return each of the paths (the keyset of {@param resultsByPath}) with all properties the AQL search returned
     * for it.
     */
    private static Map<RepoPath, Properties> pathsWithPropsFromAql(Multimap<RepoPath, AqlBaseFullRowImpl> pathRows) {
        Map<RepoPath, Properties> pathProps = Maps.newHashMap();
        for (RepoPath path : pathRows.keySet()) {
            Properties props = new PropertiesImpl();
            pathRows.get(path).stream().forEach(row -> props.put(row.getKey(), row.getValue()));
            pathProps.put(path, props);
        }
        log.trace("Returning path->properties mapping: {}", pathProps.toString());
        return pathProps;
    }

    /**
     * Populate a {@link DistributionCoordinatesResolver} with any matching groups that were resolved from the regex
     * match, if any.
     * @param filterType
     * @param filterMatcher
     * @param resolver
     */
    public static void addCaptureGroupsToRuleResolver(DistributionRuleFilterType filterType, Matcher filterMatcher,
                                                          DistributionCoordinatesResolver resolver) {
        int groupCount = filterMatcher.groupCount();
        //Group 0 (the entire pattern) is not included in the count
        if (groupCount > 0) {
            for (int i = 1; i <= groupCount; i++) {
                resolver.addCaptureGroup(filterType, filterMatcher.group(i));
            }
        }
    }

    /**
     * Creates a matcher that times out after the time specified in {@link org.artifactory.common.ConstantValues} has
     * passed to protect Artifactory from freezing a Thread over a user's regex causing catastrophic backtracking
     *
     * @param stringToMatch String that the regex will match.
     * @param regexPattern  Pattern to match against input string.
     */
    public static Matcher createTimingOutMatcher(String stringToMatch, Pattern regexPattern) {
        CharSequence charSequence = new AutoTimeoutRegexCharSequence(stringToMatch, stringToMatch,
                regexPattern.pattern(), ConstantValues.bintrayDistributionRegexTimeoutMillis.getInt());
        return regexPattern.matcher(charSequence);
    }

    public static String getValueFromToken(DistributionCoordinatesResolver coordinates, String tokenKey) {
        return coordinates.tokens.stream()
                .filter(token -> tokenKey.equals(token.getToken()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Could not resolve the value of token '" + tokenKey +
                        "' for artifact '" + coordinates.artifactPath + "' Artifactory cannot distribute without it."))
                .getValue();
    }

    public static boolean bintrayRepoExists(RepositoryHandle bintrayRepoHandle, BasicStatusHolder status) {
        try {
            if (!bintrayRepoHandle.exists()) {  //Repo exists?
                return false;
            }
        } catch (BintrayCallException bce) {
            status.error(bce.toString(), bce.getStatusCode(), log);
            return false;
        }
        return true;
    }

    /**
     * The product name token piggy-backs on the property token, it's inserted as a path property here to be evaluated
     * by the token regex mechanism later on.
     */
    public static void insertProductNameDummyProp(@Nullable String productName, Map<RepoPath, Properties> pathProperties) {
        if (StringUtils.isNotBlank(productName)) {
            pathProperties.values().stream().forEach(props -> props.put(PRODUCT_NAME_DUMMY_PROP, productName));
        }
    }

    public static boolean getIsPremiumFromResponse(HttpResponse bintrayPlanResponse) throws IOException {
        return JacksonReader.streamAsTree(bintrayPlanResponse.getEntity().getContent()).get("premium").asBoolean();
    }

    public static List<String> getLicensesFromResponse(HttpResponse bintrayLicensesResponse) throws IOException {
        return JacksonReader.streamAsTree(bintrayLicensesResponse.getEntity().getContent()).findValuesAsText("name");
    }

    /**
     * {@param distPaths} are required to be a list of full repo paths in the form "repoKey/path"
     */
    public static List<RepoPath> getPathsFromDistPathList(List<String> distPaths, BasicStatusHolder status) {
        InternalRepositoryService repoService = ContextHelper.get().beanForType(InternalRepositoryService.class);
        List<RepoPath> paths = distPaths.stream()
                .map(path -> pathToRepoPath(status, path))
                .filter(Objects::nonNull)
                .filter(repoPath -> !isCacheRepoPath(repoPath, repoService, status))
                .distinct()
                .collect(Collectors.toList());

        if (paths == null || paths.isEmpty()) {
            status.error("No paths were given to distribute", SC_BAD_REQUEST, log);
        }

        return paths;
    }

    private static boolean isCacheRepoPath(RepoPath repoPath, InternalRepositoryService repoService, BasicStatusHolder status) {
        LocalRepo localRepo = repoService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        boolean isCacheRepoPath = localRepo != null && localRepo.isCache();
        if (isCacheRepoPath) {
            status.warn("Skipping distribution from the remote repository " + repoPath, SC_BAD_REQUEST, log);
        }
        return isCacheRepoPath;
    }

    private static RepoPath pathToRepoPath(BasicStatusHolder status, String path) {
        try {
            return RepoPathFactory.create(path);
        } catch (Exception e) {
            String err = "Invalid path given: '" + path + "': ";
            status.warn(err + e.getMessage(), HttpStatus.SC_NOT_FOUND, log);
            log.debug(err, e);
        }
        return null;
    }

    /**
     * @return the path's {@link RepoType} either from the cached list of repo->type, given as
     * {@param containingRepoType} or by the type override property
     * {@link org.artifactory.util.distribution.DistributionConstants#ARTIFACT_TYPE_OVERRIDE_PROP} if it was set on the path.
     */
    public static RepoType getArtifactType(Properties pathProperties, RepoType containingRepoType, RepoPath path, BasicStatusHolder status) {
        RepoType artifactType = null;
        String typeFromProp = pathProperties.getFirst(ARTIFACT_TYPE_OVERRIDE_PROP);
        if (StringUtils.isNotBlank(typeFromProp)) {
            try {
                artifactType = RepoType.fromType(typeFromProp);
                log.debug("Found artifact type override property on path {}, overridden to type: {}", path.toPath(), artifactType);
            } catch (Exception e) {
                status.error("Invalid artifact type override property set on path " + path.toPath() + ": " + typeFromProp, log);
            }
        } else {
            artifactType = containingRepoType != null ? containingRepoType : null;
        }
        return artifactType;
    }

    public static String getTokenValueByPropKey(DistributionCoordinatesResolver resolver, String propKey) throws ItemNotFoundRuntimeException {
        //TODO [by dan]: test parallel performance?
        return resolver.tokens.stream()
                .filter(token -> token instanceof DistributionRulePropertyToken)
                .filter(token -> propKey.equals(((DistributionRulePropertyToken) token).getPropertyKey()))
                .map(DistributionRuleToken::getValue)
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new ItemNotFoundRuntimeException("Can't find property value for " + propKey +
                        " on path: " + resolver.artifactPath.toPath() + " which is required for resolving its " +
                        "distribution coordinates"));
    }

    /**
     * @return the token without the enclosing brackets.
     */
    public static String stripTokenBrackets(String token) {
        return token.replaceFirst("\\[", "").replaceFirst("\\]", "");
    }

    public static List<DistributionCoordinatesResolver> getResolversWithSameUploadInfo(
            List<DistributionCoordinatesResolver> resolvers, DistributionCoordinatesResolver resolver) {
        return resolvers.stream()
                .filter(current -> current.getRepo().equals(resolver.getRepo()))
                .filter(current -> current.getPkg().equals(resolver.getPkg()))
                .filter(current -> current.getVersion().equals(resolver.getVersion()))
                .collect(Collectors.toList());
    }

    public static BintrayUploadInfo getMergedUploadInfo(List<DistributionCoordinatesResolver> sameCoordinates) {
        return sameCoordinates.stream()
                .map(DistributionCoordinatesResolver::getBintrayUploadInfo)
                .reduce(DistributionUtils::mergeUploadInfo)
                .orElse(null);
    }

    private static BintrayUploadInfo mergeUploadInfo(BintrayUploadInfo left, BintrayUploadInfo right) {
        //None of this is supposed to be null, there's a fallback if something goes wrong
        try {
            left.getVersionDetails().setAttributes(
                    Stream.concat(left.getVersionDetails().getAttributes().stream(),
                            right.getVersionDetails().getAttributes().stream())
                            .distinct()
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            log.debug("Failed to merge upload info: {}<->{}", left != null ? left.toString() : "null",
                    right != null ? right.toString() : "null");
        }
        return left;
    }
}
