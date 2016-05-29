package org.artifactory.util.distribution;

import java.util.regex.Pattern;

/**
 * @author Dan Feldman
 */
public abstract class DistributionConstants {

    //Default Bintray repo names
    public static final String DEFAULT_DEB_REPO_NAME = "deb";
    public static final String DEFAULT_OPKG_REPO_NAME = "opkg";
    public static final String DEFAULT_VAGRANT_REPO_NAME = "boxes";
    public static final String DEFAULT_MAVEN_REPO_NAME = "maven";
    public static final String DEFAULT_NUGET_REPO_NAME = "nuget";
    public static final String DEFAULT_DOCKER_REPO_NAME = "registry";
    public static final String DEFAULT_RPM_REPO_NAME = "rpm";
    public static final String DEFAULT_GENERIC_REPO_NAME = "generic";

    //Default tokens
    public static final String PATH_TOKEN = wrapToken("artifactPath");
    public static final String PRODUCT_NAME_TOKEN = wrapToken("productName");
    public static final String PACKAGE_NAME_TOKEN = wrapToken("packageName");
    public static final String PACKAGE_VERSION_TOKEN = wrapToken("packageVersion");
    public static final String ARCHITECTURE_TOKEN = wrapToken("architecture");
    public static final String DOCKER_IMAGE_TOKEN = wrapToken("dockerImage");
    public static final String DOCKER_TAG_TOKEN = wrapToken("dockerTag");
    public static final String MODULE_TOKEN = wrapToken("module");
    public static final String BASE_REV_TOKEN = wrapToken("baseRev");
    public static final String VCS_TAG_TOKEN = wrapToken("vcsTag");
    public static final String VCS_REPO_TOKEN = wrapToken("vcsRepo");
    //Used internally to piggy-back the product name on the prop rule token
    public static final String PRODUCT_NAME_DUMMY_PROP = "internal.descriptor.product.name";
    public static final String ARTIFACT_TYPE_OVERRIDE_PROP = "distribution.package.type";
    public static final String MANIFEST_FILENAME = "manifest.json";

    public static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{[a-zA-Z]+\\}");

    public static String wrapToken(String key) {
        return "${" + key + "}";
    }
}
