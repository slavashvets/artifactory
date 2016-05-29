package org.artifactory.repo;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.jfrog.bintray.client.api.BintrayCallException;
import com.jfrog.bintray.client.api.details.PackageDetails;
import com.jfrog.bintray.client.api.details.ProductDetails;
import com.jfrog.bintray.client.api.details.RepositoryDetails;
import com.jfrog.bintray.client.api.details.VersionDetails;
import com.jfrog.bintray.client.api.handle.*;
import com.jfrog.bintray.client.api.model.Product;
import com.jfrog.bintray.client.impl.BintrayClient;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.docker.DockerAddon;
import org.artifactory.api.bintray.BintrayParams;
import org.artifactory.api.bintray.BintrayService;
import org.artifactory.api.bintray.BintrayUploadInfo;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.bintray.distribution.resolver.DistributionCoordinatesResolver;
import org.artifactory.api.bintray.distribution.rule.DistributionRuleTokens;
import org.artifactory.api.bintray.docker.BintrayDockerPushRequest;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.bintray.distribution.util.DistributionUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.md.Properties;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.db.DbLocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.PathUtils;
import org.artifactory.util.bearer.BintrayBearerPreemptiveAuthInterceptor;
import org.artifactory.util.distribution.DistributionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static org.artifactory.bintray.distribution.util.DistributionUtils.bintrayRepoExists;
import static org.artifactory.bintray.distribution.util.DistributionUtils.getValueFromToken;
import static org.artifactory.util.distribution.DistributionConstants.MANIFEST_FILENAME;

/**
 * @author Dan Feldman
 */
public class DistributionRepo extends DbLocalRepo<DistributionRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(DistributionRepo.class);

    private Bintray client;

    public DistributionRepo(DistributionRepoDescriptor descriptor, InternalRepositoryService repositoryService,
            DistributionRepo oldLocalRepo) {
        super(descriptor, repositoryService, oldLocalRepo);
    }

    @Override
    public void init() {
        super.init();
        this.client = createClient();
    }

    public BasicStatusHolder distribute(Distribution distribution) {
        BasicStatusHolder status = new BasicStatusHolder();
        verifyCanDistribute(status);
        List<RepoPath> paths = DistributionUtils.getPathsFromDistPathList(distribution.getPackagesRepoPaths(), status);
        if (status.isError()) {
            return status;
        }
        log.debug("Requested paths: {}", paths.toString());
        status.status("Starting distribution for requested paths using repo: " + distribution.getTargetRepo(), log);
        Multimap<BintrayUploadInfo, DistributionCoordinatesResolver> distCoordinates = getCoordinatesForPaths(paths, status);
        status.debug("Paths resolved to coordinates: " + distCoordinates.toString(), log);
        SubjectHandle subjectHandle = null;
        if (distCoordinates.size() > 0) {
            status.status("Distributing artifacts to Bintray...", log);
            subjectHandle = client.subject(getDescriptor().getBintrayApplication().getOrg());
        } else {
            status.error("No valid paths to distribute to Bintray, aborting.", HttpStatus.SC_BAD_REQUEST, log);
        }
        for (BintrayUploadInfo uploadInfo : distCoordinates.keySet()) {
            VersionHandle btVer;
            try {
                if (RepoType.Docker.getType().equalsIgnoreCase(uploadInfo.getRepositoryDetails().getType())) {
                    btVer = getDockerVersion(uploadInfo, subjectHandle, status);
                } else {
                    btVer = getOrCreateVersionForCoordinates(distribution.isOverrideExistingFiles(), uploadInfo, subjectHandle, status);
                }
            } catch (Exception e) {
                handleGeneralException(e, "Error creating version " + uploadInfo.toString() + ":", status);
                continue;
            }
            Collection<DistributionCoordinatesResolver> versionResolvers = distCoordinates.get(uploadInfo);
            if (versionDistributesAllArtifactsToSamePath(versionResolvers)) {
                //Dummy just to get log info from
                DistributionCoordinatesResolver dummyResolver = versionResolvers.iterator().next();
                status.error("All of the artifacts that were mapped to version " + btVer.pkg().name() + "/"
                        + btVer.name() + " by rule " + dummyResolver.ruleName + " are being mapped to the same path '" +
                        dummyResolver.getPath() + "'.  This version will not be distributed to Bintray to allow" +
                        "you to recover.", HttpStatus.SC_BAD_REQUEST, log);
                continue;
            }
            performDistribution(distribution, subjectHandle, btVer, versionResolvers, status);
        }
        return status;
    }

    private void performDistribution(Distribution distribution, SubjectHandle subjectHandle, VersionHandle btVer,
            Collection<DistributionCoordinatesResolver> versionCoordinates, BasicStatusHolder status) {
        boolean atLeastOneArtifactPushed = false;
        for (DistributionCoordinatesResolver coordinates : versionCoordinates) {
            try {
                log.debug("Deploying artifact to Bintray: {}", coordinates.toString());
                pushArtifactToBintrayCoordinates(subjectHandle, btVer, coordinates,
                        distribution.isOverrideExistingFiles());
                status.status("Successfully deployed artifact to Bintray: " + coordinates.toString(), log);
                atLeastOneArtifactPushed = true;
                copyArtifactsToDistributionRepo(coordinates, status);
            } catch (Exception e) {
                handleException(e, "Error distributing " + coordinates + ": ", status);
            }
        }
        if (atLeastOneArtifactPushed) {
            handleProductOperations(subjectHandle, btVer, status);
            String verName = btVer.pkg().name() + "/" + btVer.name();
            String gpgPassphrase = getGpgPassphrase(distribution, verName, status);
            signVersionIfNeeded(btVer, gpgPassphrase, versionCoordinates.size(), status);
            //Default publish is true
            if (distribution.isPublish() == null || (distribution.isPublish() != null && distribution.isPublish())) {
                publishFiles(btVer, gpgPassphrase, status);
            }
        }
    }

    private void verifyCanDistribute(BasicStatusHolder status) {
        AuthorizationService authService = ContextHelper.get().beanForType(AuthorizationService.class);
        if (getDescriptor().getBintrayApplication() == null) {
            status.error("Repo " + getKey() + " does not have any Bintray OAuth application defined, aborting.",
                    HttpStatus.SC_BAD_REQUEST, log);
        } else if (!authService.canDeploy(getRepoPath("."))) {
            status.error("User " + authService.currentUsername() + " Is missing the required permission to distribute" +
                    " to repo " + getKey() + ", aborting.", HttpStatus.SC_FORBIDDEN, log);
        }
    }

    private Multimap<BintrayUploadInfo, DistributionCoordinatesResolver> getCoordinatesForPaths(List<RepoPath> paths,
            BasicStatusHolder status) {
        return getDistService().coordinatesForPaths(paths, getDescriptor().getRules(), getDescriptor(), status);
    }

    private void pushArtifactToBintrayCoordinates(SubjectHandle subject, VersionHandle btVer,
            DistributionCoordinatesResolver coordinates, boolean overrideExisting) throws BintrayCallException, UnsupportedOperationException {
        switch (coordinates.type) {
            case Docker:
                //Docker push always overrides existing.
                distributeDocker(subject, coordinates);
                break;
            case Debian:
                distributeDebian(coordinates, btVer, overrideExisting);
                break;
            case Vagrant:
                distributeVagrant(coordinates, btVer, overrideExisting);
                break;
            case GitLfs:
            case Gems:
            case Pypi:
                throw new UnsupportedOperationException("Distributing packages of type " + coordinates.type +
                        " is unsupported.");
            default:
                distributeGeneric(coordinates, btVer, overrideExisting);
        }
    }

    /**
     * Create or update an existing Bintray Repository with the specified info
     *
     * @param repositoryDetails BintrayUploadInfo representing the supplied json file
     * @param subjectHandle     SubjectHandle retrieved by the Bintray Java Client
     * @param status            status holder of entire operation
     * @return a RepositoryHandle   pointing to the created/updated repository
     * @throws Exception on any unexpected error thrown by the Bintray client
     */
    public static RepositoryHandle getOrCreateRepo(RepositoryDetails repositoryDetails, SubjectHandle subjectHandle,
            BasicStatusHolder status) throws Exception {

        String repoName = repositoryDetails.getName();
        RepositoryHandle bintrayRepoHandle = subjectHandle.repository(repoName);
        try {
            if (!bintrayRepoExists(bintrayRepoHandle, status)) {
                //Repo doesn't exist - create it using the RepoDetails
                status.status("Creating repo " + repoName + " for subject " + bintrayRepoHandle.owner().name(), log);
                bintrayRepoHandle = subjectHandle.createRepo(repositoryDetails);
            } else if (repositoryDetails.getUpdateExisting() != null && repositoryDetails.getUpdateExisting()) {
                //Repo exists - update only if indicated
                status.status("Updating repo " + repoName + " with values taken from descriptor", log);
                bintrayRepoHandle.update(repositoryDetails);
            }
        } catch (BintrayCallException bce) {
            status.error(bce.getMessage() + ":" + bce.getReason(), bce.getStatusCode(), log);
            throw bce;
        } catch (IOException ioe) {
            log.debug("{}", ioe);
            throw ioe;
        }
        //Repo exists and should not be updated
        return bintrayRepoHandle;
    }

    /**
     * Create or update an existing Bintray Package with the specified info
     *
     * @param pkgDetails       PackageDetails for creating the package
     * @param repositoryHandle RepositoryHandle retrieved by the Bintray Java Client
     * @param status           status holder of entire operation
     * @return a PackageHandle pointing to the created/updated package
     * @throws Exception on any unexpected error thrown by the Bintray client
     */
    public static PackageHandle getOrCreatePackage(PackageDetails pkgDetails, RepositoryHandle repositoryHandle,
            BasicStatusHolder status, boolean updateExisting) throws Exception {
        PackageHandle packageHandle;
        packageHandle = repositoryHandle.pkg(pkgDetails.getName());
        try {
            if (!packageHandle.exists()) {
                status.status("Package " + pkgDetails.getName() + " doesn't exist, creating it", log);
                packageHandle = repositoryHandle.createPkg(pkgDetails);
            } else if (updateExisting) {
                packageHandle.update(pkgDetails);
            }
            log.debug("Package {} created", packageHandle.get().name());
        } catch (BintrayCallException bce) {
            status.error(bce.toString(), bce.getStatusCode(), bce, log);
            throw bce;
        } catch (IOException ioe) {
            log.debug("{}", ioe);
            throw ioe;
        }
        return packageHandle;
    }

    /**
     * Create or update an existing Bintray Package with the specified info
     *
     * @param versionDetails VersionDetails for creating the version
     * @param packageHandle  PackageHandle retrieved by the Bintray Java Client or by {@link #getOrCreatePackage}
     * @param status         status holder of entire operation
     * @return a VersionHandle pointing to the created/updated version
     * @throws Exception on any unexpected error thrown by the Bintray client
     */
    private VersionHandle getOrCreateVersion(VersionDetails versionDetails, PackageHandle packageHandle,
            BasicStatusHolder status, boolean updateExisting) throws Exception {
        VersionHandle versionHandle = packageHandle.version(versionDetails.getName());
        log.debug("Override existing version is set to {}", updateExisting);
        try {
            if (!versionHandle.exists()) {
                status.status("Version " + versionDetails.getName() + " doesn't exist, creating it", log);
                versionHandle = packageHandle.createVersion(versionDetails);
                log.debug("Version {} created", versionHandle.get().name());
            } else if (updateExisting) {
                //Override only version Attributes
                versionHandle.updateAttributes(versionDetails.getAttributes());
                log.debug("Version {} attributes updated", versionHandle.get().name());
            }
        } catch (BintrayCallException bce) {
            status.error(bce.toString(), bce.getStatusCode(), bce, log);
            throw bce;
        } catch (IOException ioe) {
            log.debug("{}", ioe);
            throw ioe;
        }
        return versionHandle;
    }

    private ProductHandle getOrCreateProduct(ProductDetails productDetails, SubjectHandle subjectHandle,
            BasicStatusHolder status) throws IOException {
        ProductHandle product = subjectHandle.product(productDetails.getName());
        if (!product.exists()) {
            status.status("Product " + productDetails.getName() + " doesn't exist, creating it", log);
            product = subjectHandle.createProduct(productDetails);
        }
        log.debug("Product {} created", product.name());
        return product;
    }

    private void distributeDocker(SubjectHandle subject, DistributionCoordinatesResolver coordinates) throws ItemNotFoundRuntimeException {
        BintrayDockerPushRequest dockerRequest = new BintrayDockerPushRequest();
        dockerRequest.bintrayRepo = coordinates.getRepo();
        dockerRequest.async = false;
        dockerRequest.bintraySubject = subject.name();
        dockerRequest.dockerRepository = DistributionUtils.getTokenValueByPropKey(coordinates, "docker.repoName");
        dockerRequest.dockerTagName = DistributionUtils.getTokenValueByPropKey(coordinates, "docker.manifest");
        dockerRequest.bintrayPackage = coordinates.getPkg();
        dockerRequest.bintrayTag = coordinates.getVersion();
        ContextHelper.get().beanForType(AddonsManager.class).addonByType(DockerAddon.class)
                .pushTagToBintray(coordinates.artifactPath.getRepoKey(), dockerRequest, getKey());
    }

    private void distributeDebian(DistributionCoordinatesResolver coordinates, VersionHandle btVer,
            boolean overrideExisting) throws BintrayCallException {
        String dist = getValueFromToken(coordinates, DistributionRuleTokens.Keys.debDist.key);
        String comp = getValueFromToken(coordinates, DistributionRuleTokens.Keys.debComp.key);
        String arch = getValueFromToken(coordinates, DistributionRuleTokens.Keys.architecture.key);
        btVer.uploadDebian(coordinates.getPath(), dist, comp, arch, getArtifactInputStream(coordinates), overrideExisting);
    }

    private void distributeVagrant(DistributionCoordinatesResolver coordinates, VersionHandle btVer,
            boolean overrideExisting) throws BintrayCallException {
        String boxProvider = getValueFromToken(coordinates, DistributionRuleTokens.Keys.vagrantProvider.key);
        btVer.uploadVagrant(coordinates.getPath(), boxProvider, getArtifactInputStream(coordinates), overrideExisting);
    }

    private void distributeGeneric(DistributionCoordinatesResolver coordinates, VersionHandle btVer,
            boolean overrideExisting) throws BintrayCallException {
        btVer.upload(coordinates.getPath(), getArtifactInputStream(coordinates), overrideExisting);
    }

    /**
     * Creates the product if required and tests if the package is already contained in it (if it existed) - will add
     * new packages to the product as needed
     */
    private void handleProductOperations(SubjectHandle subject, VersionHandle btVer, BasicStatusHolder status) {
        String productName = getDescriptor().getProductName();
        if (StringUtils.isBlank(productName)) {
            return;
        }
        ProductDetails productDetails = new ProductDetails();
        productDetails.setName(productName);
        ProductHandle productHandle;
        try {
            productHandle = getOrCreateProduct(productDetails, subject, status);
        } catch (Exception e) {
            handleException(e, "Error getting (or creating) product " + productName, status);
            return;
        }
        attachPackagesToProduct(btVer, productHandle, status);
    }

    private void attachPackagesToProduct(VersionHandle btVer, ProductHandle productHandle, BasicStatusHolder status) {
        try {
            Product product = productHandle.get();
            String pkgForProduct = btVer.pkg().repository().name() + "/" + btVer.pkg().name();
            if (!product.getPackages().contains(pkgForProduct)) {
                status.status("Package" + pkgForProduct + " not contained in product " + product.getName() +
                        ", adding it.", log);
                productHandle.addPackages(Lists.newArrayList(pkgForProduct));
            } else {
                status.status("Package" + pkgForProduct + " already contained in product " + product.getName(), log);
            }
        } catch (Exception e) {
            handleException(e, "Error attaching distributed packages to product " + productHandle.name(), status);
        }
    }

    /**
     * Signs all files in {@param version} except for maybe metadata - see doc of {@link DistributionRepo#publishFiles}
     */
    private void signVersionIfNeeded(VersionHandle version, String gpgPassphrase, int versionFileCount, BasicStatusHolder status) {
        try {
            //BLank string means gpg sign without password
            if (gpgPassphrase != null && gpgPassphrase.equals("")) {
                version.sign(versionFileCount);
            } else if (gpgPassphrase != null){
                version.sign(gpgPassphrase, versionFileCount);
            }
        } catch (Exception e) {
            handleException(e, "Error signing version " + version.pkg().name() + "/" + version.name(), status);
        }
    }

    /**
     * Publishes all files in {@param version}, Also signs metadata if required.
     * The way this flow works in Bintray forces us to publish + sign (with the header) if we want metadata files
     * (i.e. debian metadata) signed and the sign AGAIN if we want the actual artifacts signed.
     */
    private void publishFiles(VersionHandle version, String gpgPassphrase, BasicStatusHolder status) {
       String verName = version.pkg().name() + "/" + version.name();
       status.status("Publishing files in version: " + verName, log);
       try {
           if (gpgPassphrase != null) {
               version.publish(gpgPassphrase);
           } else {
               version.publish();
           }
       } catch (BintrayCallException bce) {
           status.error(bce.toString(), bce.getStatusCode(), log);
       }
    }

    /**
     * Outputs the status of this request's signing info - whether the user passed it or the descriptor demands it
     * and returns the passphrase itself if signing is required for this version
     * NOTE: If the descriptor requires signing but no passphrase is present this method returns an empty string to
     * signify that and allow the publish and sign operations to attempt to sign without a passphrase.
     */
    private
    @Nullable
    String getGpgPassphrase(Distribution distribution, String verName, BasicStatusHolder status) {
        String passphrase = null;
        if (StringUtils.isNotBlank(distribution.getGpgPassphrase())) {
            //passing the passphrase from api overrides anything
            status.status("GPG Passphrase was passed to the command - version " + verName + " will be signed", log);
            passphrase = distribution.getGpgPassphrase();
        } else if (getDescriptor().isGpgSign()) {
            if (StringUtils.isNotBlank(getDescriptor().getGpgPassPhrase())) {
                //descriptor says sign and has passphrase
                status.status("Repository " + getDescriptor().getKey() + " is configured to automatically sign versions"
                        + " - version " + verName + " will be signed", log);
                passphrase = getDescriptor().getGpgPassPhrase();
            } else {
                //descriptor says sign and no passphrase - attempt to sign anyway the private key might not be required
                status.status("Repository " + getDescriptor().getKey() + " is configured to automatically sign versions"
                        + " and no passphrase was given - attempting to sign version " + verName + " without " +
                        "a passphrase", log);
                passphrase = "";
            }
        }
        return passphrase;
    }

    /**
     * Moves or Copies the artifacts that were distributed using {@param coordinates} to this repo under the appropriate
     * tree structure.
     * Also writes the coordinates as properties on the artifacts.
     */
    private void copyArtifactsToDistributionRepo(DistributionCoordinatesResolver coordinates, BasicStatusHolder status) {
        RepoPath pathToMove = coordinates.artifactPath;
        RepoPath artifactoryDistributionPath;
        RepoPath propertyWritePath;
        if (coordinates.type.equals(RepoType.Docker)) {
            //Path should point to the manifest - so move the parent (only docker v2 is supported)
            pathToMove = pathToMove.getParent();
            artifactoryDistributionPath = RepoPathFactory.create(getKey(), Joiner.on("/").join(coordinates.getRepo(),
                    coordinates.getPkg(), coordinates.getVersion()));
            propertyWritePath = new RepoPathImpl(artifactoryDistributionPath, MANIFEST_FILENAME);
            //If parent path exists the copy will put it under existingPath/existingPath instead of overriding so adjust
            //the target path to the parent so we override the existing artifact.
            if (getRepositoryService().exists(artifactoryDistributionPath) && !artifactoryDistributionPath.isRoot()) {
                artifactoryDistributionPath = artifactoryDistributionPath.getParent();
            }
        } else {
            artifactoryDistributionPath = RepoPathFactory.create(getKey(), Joiner.on("/").join(coordinates.getRepo(),
                    coordinates.getPkg(), coordinates.getVersion(), coordinates.getPath()));
            propertyWritePath = artifactoryDistributionPath;
        }
        status.status("Copying artifact " + pathToMove + " to distribution repository under path "
                + artifactoryDistributionPath, log);
        if (pathToMove != null && artifactoryDistributionPath != null) {
            String sourcePath = pathToMove.toPath();
            String targetPath = artifactoryDistributionPath.toPath();
            if (sourcePath.equals(targetPath)) {
                log.debug("Artifact was re-distributed (same source '{}' and target '{}' paths given to copy operation),"
                        + "skipping copy of distributed artifact.", sourcePath, targetPath);
            } else {
                status.merge(getRepositoryService().copy(pathToMove, artifactoryDistributionPath, false, true, false));
            }
        }
        status.status("Setting target distribution coordinates as properties on path " + propertyWritePath, log);
        if (ContextHelper.get().beanForType(AuthorizationService.class).canAnnotate(propertyWritePath)) {
            writeBintrayCoordinatesProps(propertyWritePath, coordinates, status);
            //TODO [by dan]: need to support showing package type on folder node that represents Bintray repo in phase 2
            // setPackageTypePropOnBtRepoFolder(propertyWritePath, coordinates);
        } else {
            status.warn("Can't write Bintray coordinates on artifact " + propertyWritePath + ": user lacks annotation "
                    + "permission on path.", HttpStatus.SC_FORBIDDEN, log);
        }
    }

    /**
     * Create a path for the artifact's grandparent - it represents the Bintray repo the artifact was pushed to and
     * we set the package type there as well for the ui info tab.
     *
     * @param propertyWritePath Path of where the artifact was copied to.
     */
    private void setPackageTypePropOnBtRepoFolder(RepoPath propertyWritePath, DistributionCoordinatesResolver coordinates) {
        try {
            RepoPath bintrayRepoAncestor = RepoPathFactory.create(propertyWritePath.getRepoKey(),
                    PathUtils.getAncesstor(propertyWritePath.getPath(), 3));
            Properties typeProps = new PropertiesImpl();
            typeProps.put(DistributionConstants.ARTIFACT_TYPE_OVERRIDE_PROP, coordinates.type.name());
            getRepositoryService().setProperties(bintrayRepoAncestor, typeProps);
        } catch (Exception e) {
            //User doesn't care about our meta props
            log.debug("Error setting properties on path {}", propertyWritePath);
        }
    }

    private void writeBintrayCoordinatesProps(RepoPath pathToWrite, DistributionCoordinatesResolver coordinates,
            BasicStatusHolder status) {
        BintrayParams btParams = new BintrayParams();
        btParams.setRepo(coordinates.getRepo());
        btParams.setPackageId(coordinates.getPkg());
        btParams.setVersion(coordinates.getVersion());
        btParams.setPath(coordinates.getPath());
        btParams.setPackageType(coordinates.type.name());
        try {
            getBintrayService().savePropertiesOnRepoPath(pathToWrite, btParams);
        } catch (Exception e) {
            status.warn("Error setting Bintray coordinates as properties on path " + pathToWrite,
                    HttpStatus.SC_BAD_REQUEST, log);
        }
    }

    public Bintray getClient() {
        return client;
    }

    /**
     * @return A VersionHandle for the version that will be created in Bintray for these {@param coordinates}, will
     * get or create the repo, package and version if needed.
     */
    private VersionHandle getOrCreateVersionForCoordinates(boolean overrideExistingVersions, BintrayUploadInfo uploadInfo,
            SubjectHandle subject, BasicStatusHolder status) throws Exception {
        RepositoryHandle btRepo = getOrCreateRepo(uploadInfo.getRepositoryDetails(), subject, status);
        //pkg defaults are only for creation, we don't override
        PackageHandle btPkg = getOrCreatePackage(uploadInfo.getPackageDetails(), btRepo, status, false);
        return getOrCreateVersion(uploadInfo.getVersionDetails(), btPkg, status, overrideExistingVersions);
    }

    /**
     * @return A VersionHandle for the version that will be created in Bintray for these {@param coordinates}, will
     * only get or create the repo as the package and version are created by the Bintray Docker Pusher.
     */
    private VersionHandle getDockerVersion(BintrayUploadInfo uploadInfo, SubjectHandle subject, BasicStatusHolder status) throws Exception {
        //For Docker, only need to create repo, package and version are created by the docker pusher later.
        RepositoryHandle btRepo = getOrCreateRepo(uploadInfo.getRepositoryDetails(), subject, status);
        return btRepo.pkg(uploadInfo.getPackageDetails().getName()).version(uploadInfo.getVersionDetails().getName());
    }

    private Bintray createClient() {
        return createBintrayClient(new HttpClientConfigurator()
                .hostFromUrl(getBaseBintrayApiUrl())
                .soTimeout(ConstantValues.bintrayClientDistributionRequestTimeout.getInt())
                .connectionTimeout(ConstantValues.bintrayClientDistributionRequestTimeout.getInt())
                .noRetry()
                .proxy(getProxy())
                .maxTotalConnections(30)
                .defaultMaxConnectionsPerHost(30)
                .enableTokenAuthentication(true, getKey(), new BintrayBearerPreemptiveAuthInterceptor(getKey()))
                .getClient());
    }

    private Bintray createBintrayClient(CloseableHttpClient httpClient) {
        return BintrayClient.create(httpClient, PathUtils.trimTrailingSlashes(getBaseBintrayApiUrl()),
                ConstantValues.bintrayClientThreadPoolSize.getInt(),
                ConstantValues.bintrayClientSignRequestTimeout.getInt());
    }

    private String getBaseBintrayApiUrl() {
        return PathUtils.addTrailingSlash(ConstantValues.bintrayApiUrl.getString());
    }

    private ProxyDescriptor getProxy() {
        return getDescriptor().getProxy();
    }

    private DistributionService getDistService() {
        return ContextHelper.get().beanForType(DistributionService.class);
    }

    private BintrayService getBintrayService() {
        return ContextHelper.get().beanForType(BintrayService.class);
    }

    private InputStream getArtifactInputStream(DistributionCoordinatesResolver coordinates) {
        return getRepositoryService().getResourceStreamHandle(coordinates.artifactPath).getInputStream();
    }

    private void handleException(Exception e, String errMsg, BasicStatusHolder status) {
        if (e instanceof BintrayCallException) {
            handleBintrayException((BintrayCallException) e, errMsg, status);
        } else {
            handleGeneralException(e, errMsg, status);
        }
    }

    private void handleGeneralException(Exception e, String err, BasicStatusHolder status) {
        log.debug(err, e);
        //Might be a wrapped Bintray Exception thrown by the token provider
        Throwable btCause = ExceptionUtils.getCauseOfTypes(e, BintrayCallException.class);
        if (btCause != null) {
            BintrayCallException bce = (BintrayCallException) btCause;
            status.error(err + e.getMessage() + " - " + bce.toString(), bce.getStatusCode(), log);
        } else {
            status.error(err + e.getMessage(), log);
        }
    }

    private void handleBintrayException(BintrayCallException bce, String err, BasicStatusHolder status) {
        log.debug(err, bce);
        status.error(err + bce.toString(), bce.getStatusCode(), log);
    }

    /**
     * Identifies a user error that will cause all artifacts that are distributed to a version {@param versionResolvers}
     * to be deployed to the exact same path.
     */
    private boolean versionDistributesAllArtifactsToSamePath(Collection<DistributionCoordinatesResolver> versionResolvers) {
        long distinctPaths = versionResolvers.stream()
                .map(DistributionCoordinatesResolver::getPath)
                .distinct()
                .count();
        //Less distinct paths then all paths --> there were duplicate paths
        return (distinctPaths < versionResolvers.size()) && versionResolvers.size() > 1;
    }
}
