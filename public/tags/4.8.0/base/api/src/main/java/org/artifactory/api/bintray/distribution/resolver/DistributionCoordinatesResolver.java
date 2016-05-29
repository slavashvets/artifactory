package org.artifactory.api.bintray.distribution.resolver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jfrog.bintray.client.api.details.Attribute;
import com.jfrog.bintray.client.api.details.PackageDetails;
import com.jfrog.bintray.client.api.details.RepositoryDetails;
import com.jfrog.bintray.client.api.details.VersionDetails;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.bintray.BintrayUploadInfo;
import org.artifactory.api.bintray.distribution.rule.DistributionRulePropertyToken;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleToken;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleTokens;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.distribution.DistributionCoordinates;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.distribution.DistributionConstants;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.artifactory.api.bintray.BintrayService.*;
import static org.artifactory.api.bintray.distribution.resolver.DistributionRuleFilterType.GENERAL_CAP_GROUP_PATTERN;
import static org.artifactory.util.distribution.DistributionConstants.ARTIFACT_TYPE_OVERRIDE_PROP;
import static org.artifactory.util.distribution.DistributionConstants.TOKEN_PATTERN;

/**
 * Distribution Coordinates implementation that holds it's own tokens (with values where applicable), capturing groups
 * if any were defined in the filters and can replace all tags in it's coordinate fields with actual values
 *
 * @author Dan Feldman
 */
public class DistributionCoordinatesResolver extends DistributionCoordinates {
    private static final Logger log = LoggerFactory.getLogger(DistributionCoordinatesResolver.class);

    private final static String REPO_FIELD = "repo";
    private final static String PACKAGE_FIELD = "package";
    private final static String VERSION_FIELD = "version";
    private final static String PATH_FIELD = "path";

    @JsonIgnore
    public RepoPath artifactPath; //Path (in Artifactory) of artifact that will be distributed.

    @JsonIgnore
    public List<DistributionRuleToken> tokens = new ArrayList<>(); //All related tokens and their values

    @JsonIgnore
    public RepoType type; //Type of the distributed artifact.

    @JsonIgnore
    public BintrayUploadInfo uploadInfo; //Info for bintray repo/pkg/version creation

    @JsonIgnore
    private Map<DistributionRuleFilterType, List<String>> capturingGroups = initCaptureGroups(); //Values of the capturing groups extracted from the rule.

    private Map<DistributionRuleFilterType, List<String>> initCaptureGroups() {
        Map<DistributionRuleFilterType, List<String>> map = Maps.newHashMap();
        Stream.of(DistributionRuleFilterType.values()).forEach((type) -> map.put(type, Lists.newArrayList()));
        return Collections.unmodifiableMap(map);
    }

    @JsonIgnore
    private Properties pathProperties;

    public DistributionCoordinatesResolver(DistributionRule rule, RepoPath path, @Nonnull Properties pathProperties) {
        super(rule.getDistributionCoordinates());
        this.artifactPath = path;
        this.type = rule.getType();
        this.tokens = DistributionRuleTokens.tokensByType(type);
        this.ruleName = rule.getName();
        this.pathProperties = pathProperties;
    }

    @JsonIgnore
    public String ruleName; //The rule these resolver maps

    @JsonIgnore
    public RepoPath getArtifactPath() {
        return artifactPath;
    }

    @JsonIgnore
    public BintrayUploadInfo getBintrayUploadInfo() {
        return uploadInfo;
    }

    /**
     * adds {@param group} to the capturing groups list of the given filter type - maintains insertion order so that
     * the token ${qualifier:1} will resolve to capturingGroups[type](0)
     */
    public void addCaptureGroup(DistributionRuleFilterType filterType, String group) {
        this.capturingGroups.get(filterType).add(group);
    }

    /**
     * Resolves the target Bintray coordinates by populating all tokens available to this resolver with properties set
     * on the path it's supposed to resolve, or replace the fields with pre-existing Bintray coordinates that were
     * already set on the path as properties as well.
     *
     * If no pre-existing coordinates exist, and tokens are populated with values, this method also replaces all tokens
     * in each coordinate field of this resolver.
     */
    public DistributionCoordinatesResolver resolve(BasicStatusHolder status) {
        if (pathProperties != null) {
            if (pathHasDistributionCoordinates(pathProperties)) {
                status.status("Path " + artifactPath.toPath() + " has pre-existing Bintray distribution coordinates."
                        + " it will be distributed according to them.", log);
                replaceFieldsWithExistingCoordinates(status);
            } else {
                populateTokenValues(status);
                replaceTokensWithValues(status);
            }
        }
        return validateNoTokensLeftInFields(status);
    }

    public static boolean pathHasDistributionCoordinates(Properties pathProperties) {
        return pathProperties.containsKey(BINTRAY_REPO) && pathProperties.containsKey(BINTRAY_PACKAGE)
                && pathProperties.containsKey(BINTRAY_VERSION) && pathProperties.containsKey(BINTRAY_PATH);
    }

    /**
     * Assigns a value for each token based on what's in {@param pathProperties}
     */
    private void populateTokenValues(BasicStatusHolder status) {
        for (DistributionRuleToken token : tokens) {
            try {
                token.populateValue(artifactPath, pathProperties);
            } catch (Exception e) {
                String err = e.getMessage() + " - in rule " + ruleName;
                status.warn(err, 400, log);
                log.debug(err, e);
            }
        }
    }

    /**
     * replaces all field of this resolver with the pre-existing coordinates (bintray.repo/package/version/path) in
     * {@param pathProperties} to support the re-distribute action
     * use only if all properties are set on the path!
     */
    private void replaceFieldsWithExistingCoordinates(BasicStatusHolder status) {
        this.repo = pathProperties.getFirst(BINTRAY_REPO);
        this.pkg = pathProperties.getFirst(BINTRAY_PACKAGE);
        this.version = pathProperties.getFirst(BINTRAY_VERSION);
        this.path = pathProperties.getFirst(BINTRAY_PATH);
        try {
            this.type = RepoType.fromType(pathProperties.getFirst(ARTIFACT_TYPE_OVERRIDE_PROP));
        } catch (Exception e) {
            status.warn("Failed to retrieve artifact path from expected property " + ARTIFACT_TYPE_OVERRIDE_PROP +
                    ". artifact " + artifactPath + " will be distributed with the existing coordinates but as "
                    + "generic", 404, log);
        }
        try {
            //Push to Bintray sets the repo property as 'subject/repo' distribution does not use the subject.
            if (repo.contains("/")) {
                log.debug("Found old Bintray repo coordinate property for Bintray repo: {}, removing subject" +
                        " from field for resolution", repo);
                repo = repo.split("/")[1];
            }
        } catch (Exception e) {
            status.warn("Failed to get subject from old bintray repo coordinate property: " + repo + ": "
                    + e.getMessage() + ". the repo coordinate will be used as-is.", log);
        }
        log.debug("Overriding coordinates by rule for path {}, based on existing distribution coordinates set on " +
                        "it: repo -> {}, package -> {}, version -> {}, path -> {}", artifactPath.toPath(), repo, pkg,
                version, path);
    }

    /**
     * Replaces all tokens in each of the coordinate fields (repo, package, version, path) with actual values,
     * according to the tokens available to this rule (given as {@param tokens}) and the capturing groups
     * (given as {@param capturingGroups}) extracted from the rule's path filter, if any.
     */
    private DistributionCoordinatesResolver replaceTokensWithValues(BasicStatusHolder status) {
        try {
            for (DistributionRuleToken token : tokens) {
                repo = replaceTokenInField(token, repo, REPO_FIELD);
                pkg = replaceTokenInField(token, pkg, PACKAGE_FIELD);
                version = replaceTokenInField(token, version, VERSION_FIELD);
                path = replaceTokenInField(token, path, PATH_FIELD);
            }
            for (DistributionRuleFilterType filterType : DistributionRuleFilterType.values()) {
                if (CollectionUtils.isNotEmpty(capturingGroups.get(filterType))) {
                    repo = replaceCaptureGroupsInField(filterType, repo, REPO_FIELD);
                    pkg = replaceCaptureGroupsInField(filterType, pkg, PACKAGE_FIELD);
                    version = replaceCaptureGroupsInField(filterType, version, VERSION_FIELD);
                    path = replaceCaptureGroupsInField(filterType, path, PATH_FIELD);
                }
            }
        } catch (Exception e) {
            String err = (e.getMessage() == null ? "An error occurred while resolving distribution " +
                    "coordinates for artifact " + artifactPath.toPath() : e.getMessage())
                    + " - in rule " + ruleName;
            status.warn(err + ". Check the log for more details.", 400, log);
            log.debug(err, e);
        }
        return this;
    }

    private String replaceTokenInField(DistributionRuleToken token, String field, String fieldName) throws Exception {
        if (field.contains(token.getToken())) {
            if (token.getValue() == null) {
                String err = "Failing rule " + ruleName + " - No value present for token " + token.getToken() +
                        " that was found in field '" + fieldName + "' for artifact " + artifactPath.toPath();
                if (token instanceof DistributionRulePropertyToken
                        && !token.getToken().equals(DistributionConstants.PRODUCT_NAME_TOKEN)) {
                    err += "Verify that this package has been indexed and property " +
                            ((DistributionRulePropertyToken) token).getPropertyKey() + " is set correctly.";
                }
                throw new Exception(err);
            }
            log.debug("Found token {} in '{}' field: {} for artifact {}. replacing with value {}", token.getToken(),
                    fieldName, artifactPath.toPath(), field, token.getValue());
            Matcher fieldMatcher = Pattern.compile(token.getToken(), Pattern.LITERAL).matcher(field);
            return fieldMatcher.replaceAll(token.getValue());
        }
        return field;
    }

    private String replaceCaptureGroupsInField(DistributionRuleFilterType filterType, String field, String fieldName) throws Exception {
        Matcher capGroupMatcher = filterType.getCaptureGroupPattern().matcher(field);
        while (capGroupMatcher.find()) {
            String group = capGroupMatcher.group(0);
            log.debug("Found group token {} in {} field {}. trying to parse group number", group, fieldName, field);
            int groupNum = filterType.getGroupNumber(group);
            try {
                //group 1 is position 0 in the array
                String val = capturingGroups.get(filterType).get(groupNum - 1);
                log.debug("Replacing group token {} with value {}", group, val);
                field = field.replaceAll(Pattern.quote(group), val);
            } catch (IndexOutOfBoundsException obe) {
                throw new Exception("No capturing group found with number " + groupNum + " for field " + fieldName +
                        " : " + field + ". Failing distribution rule '" + ruleName + "'.");
            }
        }
        capGroupMatcher = capGroupMatcher.reset(field);
        //Field still has unmatched capture group tokens - fail the rule
        if (capGroupMatcher.find()) {
            throw new Exception("Couldn't match all capture group tokens in field " + fieldName + ": " + field
                    + ". Failing distribution rule '" + ruleName + "'.");
        }
        return field;
    }

    /**
     * Validates no tokens are left in the coordinate fields - will fail the rule if any tokens are left.
     */
    private DistributionCoordinatesResolver validateNoTokensLeftInFields(BasicStatusHolder status) {
        boolean hasTokens = false;
        String err = "Coordinate Field %s in rule '" + ruleName + "' contains tokens that were not matched: %s" +
                " for artifact " + artifactPath.toPath() + ", failing this rule.";
        if (containsToken(repo)) {
            hasTokens = true;
            status.error(String.format(err, REPO_FIELD, repo), 400, log);
        } else if (containsToken(pkg)) {
            hasTokens = true;
            status.error(String.format(err, PACKAGE_FIELD, pkg), 400, log);
        } else if (containsToken(version)) {
            hasTokens = true;
            status.error(String.format(err, VERSION_FIELD, version), 400, log);
        } else if (containsToken(path)) {
            hasTokens = true;
            status.error(String.format(err, PATH_FIELD, path), 400, log);
        }
        if (hasTokens) {
            return null;
        }
        return this;
    }

    /**
     * @return true if the given string (representing a field in the coordinates resolver) contains a token
     */
    private static boolean containsToken(String field) {
        return TOKEN_PATTERN.matcher(field).find() || GENERAL_CAP_GROUP_PATTERN.matcher(field).find();
    }

    @JsonIgnore
    public DistributionCoordinatesResolver populateUploadInfo(DistributionRepoDescriptor descriptor) {
        BintrayUploadInfo info = new BintrayUploadInfo();

        //repo
        RepositoryDetails btRepo = new RepositoryDetails();
        btRepo.setName(repo);
        setBintrayRepoType(btRepo);
        btRepo.setOwner(descriptor.getBintrayApplication().getOrg());
        btRepo.setIsPrivate(descriptor.getDefaultNewRepoPrivate());
        btRepo.setPremium(descriptor.getDefaultNewRepoPremium());
        btRepo.setUpdateExisting(false);
        info.setRepositoryDetails(btRepo);

        //pkg
        PackageDetails btPkg = new PackageDetails(pkg);
        if (CollectionUtils.isNotEmpty(descriptor.getDefaultLicenses())) {
            btPkg.licenses(Lists.newArrayList(descriptor.getDefaultLicenses()));
        }
        if (StringUtils.isNotBlank(descriptor.getDefaultVcsUrl())) {
            btPkg.vcsUrl(descriptor.getDefaultVcsUrl());
        }
        info.setPackageDetails(btPkg);

        //version
        VersionDetails versionDetails = new VersionDetails(version);
        versionDetails.setAttributes(getWhitelistProperties(descriptor));
        info.setVersionDetails(versionDetails);

        this.uploadInfo = info;
        return this;
    }

    private List<Attribute> getWhitelistProperties(DistributionRepoDescriptor descriptor) {
        return descriptor.getWhiteListedProperties().stream()
                .filter(pathProperties::containsKey)
                .map(propKey -> new Attribute<>(propKey, Attribute.Type.string, Lists.newArrayList(pathProperties.get(propKey))))
                .collect(Collectors.toList());
    }

    private void setBintrayRepoType(RepositoryDetails btRepo) {
        switch (type) {
            case YUM:
                btRepo.setType("rpm");
                break;
            case Maven:
            case Ivy:
            case SBT:
            case Gradle:
                btRepo.setType("maven");
                break;
            case NuGet:
                btRepo.setType("nuget");
                break;
            case Vagrant:
                btRepo.setType("vagrant");
                break;
            case Debian:
                btRepo.setType("debian");
                break;
            case Opkg:
                btRepo.setType("opkg");
                break;
            case Docker:
                btRepo.setType("docker");
                break;
            default:
                btRepo.setType("generic");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DistributionCoordinatesResolver)) return false;
        if (!super.equals(o)) return false;
        DistributionCoordinatesResolver resolver = (DistributionCoordinatesResolver) o;
        return artifactPath != null ? artifactPath.equals(resolver.artifactPath) : resolver.artifactPath == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (artifactPath != null ? artifactPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return artifactPath + " -> " + repo + "/" + pkg + "/" + version + "/" + path;
    }
}
