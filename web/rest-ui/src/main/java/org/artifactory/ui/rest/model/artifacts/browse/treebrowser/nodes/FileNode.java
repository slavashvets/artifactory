package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes;

import org.artifactory.addon.AddonType;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.bower.BowerAddon;
import org.artifactory.addon.pypi.PypiAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.ivy.IvyNaming;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.model.artifact.BaseArtifact;
import org.artifactory.rest.common.model.artifact.IAction;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.util.BintrayRestHelper;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.IArtifactInfo;
import org.artifactory.util.PathUtils;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Chen Keinan
 */
@JsonTypeName("file")
public class FileNode extends BaseNode {

    private FileInfo fileInfo;
    private String type ="file";
    private String mimeType;

    FileNode(){}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FileNode(FileInfo fileInfo,String text) {
        super(fileInfo.getRepoPath());
        this.fileInfo = fileInfo;
        super.setText(text);
        setLocal(true);
    }


    @Override
    public void populateTabs(AuthorizationService authorizationService) {
        List<IArtifactInfo> tabs = super.fetchTabs();
        if (isLocal()) {
            if (isDistribution()) {
                boolean canAdmin = authorizationService.canManage(getRepoPath());
                addGeneralTab(tabs);
                addEffectivePermissionTab(tabs, canAdmin);
                addPropertiesTab(tabs);
                addWatchTab(tabs, canAdmin);
            } else if (isTrash()) {
                addGeneralTab(tabs);
            } else {
                AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
                RepoPath repoPath = fetchRepoPath();
                ItemInfo itemInfo = this.retrieveItemInfo(getRepoPath());
                boolean canAdmin = authorizationService.canManage(repoPath);
                Properties properties = getRepoService().getProperties(getRepoPath());
                //add addons  tabs
                addGeneralTab(tabs);
                addPomTab(tabs);
                addXmlTab(tabs);
                //addJnplTab(tabs);
                addRpmTab(tabs, addonsManager);
                addNuGetTab(tabs, addonsManager);
                addRubyGemsTab(tabs, addonsManager);
                addNpmTab(tabs, addonsManager, properties);
                addBowerTab(tabs, addonsManager, properties);
                addPypiTab(tabs, addonsManager, properties);
                addEffectivePermissionTab(tabs, canAdmin);
                addPropertiesTab(tabs);
                addWatchTab(tabs, canAdmin);
                addBuildTab(authorizationService, tabs, itemInfo);
                addBlackDuckTab(tabs, itemInfo);
            }
        }
    }


    /**
     * populate black duck tab
     *
     * @param tabs     - tabs list
     * @param itemInfo - item info
     */
    private void addBlackDuckTab(List<IArtifactInfo> tabs, ItemInfo itemInfo) {
        if (!itemInfo.isFolder()) {
            tabs.add(new BaseArtifactInfo("BlackDuck"));
        }
    }

    /**
     * populate build tab
     *
     * @param authService - authorization service
     * @param tabs        -tabs list
     * @return - item info
     */
    private void addBuildTab(AuthorizationService authService, List<IArtifactInfo> tabs, ItemInfo itemInfo) {
        if (!itemInfo.isFolder() && itemInfo instanceof MutableFileInfo
                && !authService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            tabs.add(new BaseArtifactInfo("Builds"));
        }
    }

    @Override
    public void populateActions(AuthorizationService authService) {
        RepoPath repoPath = InternalRepoPathFactory.create(getRepoKey(), getPath());
        boolean canRead = authService.canRead(repoPath);
        boolean canDelete = authService.canDelete(repoPath);
        updateFileInfo();
        List<IAction> actions = new ArrayList<>();
        if (isDistribution()) {
            addWatchAction(authService, actions, canRead);
            addDistributionActions(actions);
            addDeleteAction(actions, canDelete);
        } else if (isTrash()) {
            addRestoreAction(actions, authService.isAdmin());
            addDeleteAction(actions, authService.isAdmin());
        } else {
            addDownloadAction(actions);
            addViewAction(actions);
            addCopyAction(authService, actions, repoPath);
            addMoveAction(authService, actions, repoPath, canDelete);
            addWatchAction(authService, actions, canRead);
            addDistributionActions(actions);
            addBintrayActions(fileInfo, actions);
            addDeleteAction(actions, canDelete);
        }
        setActions(actions);
    }

    private void addBintrayActions(ItemInfo itemInfo, List<IAction> actions) {
        if (!itemInfo.isFolder()) {
            if (repoCanPushToBintray() && BintrayRestHelper.isPushToBintrayAllowed(null, null)) {
                actions.add(new BaseArtifact("UploadToBintray"));
            }
        }
    }

    @Override
    protected RepoPath fetchRepoPath() {
        return fileInfo.getRepoPath();
    }

    /**
     * add rpm tab
     *
     * @param tabs - tabs list
     */
    private void addRpmTab(List<IArtifactInfo> tabs, AddonsManager addonsManager) {
        boolean repoSupportsRpm = isRepoSupportType(RepoType.YUM);
        if (isRpmFile() && repoSupportsRpm && addonsManager.isAddonSupported(AddonType.YUM)) {
            tabs.add(new BaseArtifactInfo("Rpm"));
        }
    }

    /**
     * add jnpl tabs
     *
     * @param tabs - tabs list
     */
    private void addJnplTab(List<IArtifactInfo> tabs) {
        if (isJnlpFile()) {
            tabs.add(new BaseArtifactInfo("JNPL"));
        }
    }

    /**
     * add xml tab
     *
     * @param tabs list
     */
    private void addXmlTab(List<IArtifactInfo> tabs) {
        if (isXmlFile() && !isPomFile()) {
                //xml tab
            IArtifactInfo xmlTab = isIvyFile() ? new BaseArtifactInfo("IVYXml") : new BaseArtifactInfo("GeneralXml");
                tabs.add(xmlTab);
        }
    }

    /**
     * add nuGet tab
     *
     * @param tabs          - tabs list
     * @param addonsManager - add on managers
     */
    private void addNuGetTab(List<IArtifactInfo> tabs, AddonsManager addonsManager) {
        boolean repoSupportsNuget = isRepoSupportType(RepoType.NuGet);
        if (isNuPkgFile() && repoSupportsNuget && addonsManager.isAddonSupported(AddonType.NUGET)) {
            tabs.add(new BaseArtifactInfo("NuPkgInfo"));
        }
    }

    /**
     * add RubyGems tab
     *
     * @param tabs          - tabs list
     * @param addonsManager - add on managers
     */
    private void addRubyGemsTab(List<IArtifactInfo> tabs, AddonsManager addonsManager) {
        boolean repoSupportsGem = isRepoSupportType(RepoType.Gems);
        if (isGemFile() && repoSupportsGem && addonsManager.isAddonSupported(AddonType.GEMS)) {
            tabs.add(new BaseArtifactInfo("RubyGems"));
        }
    }

    /**
     * add Pypi tab
     *
     * @param tabs          - tabs list
     * @param addonsManager - add on managers
     */
    private void addPypiTab(List<IArtifactInfo> tabs, AddonsManager addonsManager, Properties properties) {
        boolean repoSupportsPypi = isRepoSupportType(RepoType.Pypi);
        if (isPypiFile(properties) && repoSupportsPypi && addonsManager.isAddonSupported(AddonType.PYPI)) {
            tabs.add(new BaseArtifactInfo("PyPIInfo"));
        }
    }

    /**
     * add ppom tab
     *
     * @param tabs - tabs list
     */
    private void addPomTab(List<IArtifactInfo> tabs) {
        if (isPomFile()) {
            // add pom view panel
            tabs.add(new BaseArtifactInfo("PomView"));
        }
    }

    /**
     * add Npm tab
     *
     * @param tabs          - tabs list
     * @param addonsManager - add on managers
     */
    private void addNpmTab(List<IArtifactInfo> tabs, AddonsManager addonsManager, Properties properties) {
        boolean isNpmFileTypeAndSupported = isRepoSupportType(RepoType.Npm) && isNpmFile(properties)
                && addonsManager.isAddonSupported(AddonType.NPM);
        if (isNpmFileTypeAndSupported) {
            tabs.add(new BaseArtifactInfo("NpmInfo"));
        }
    }

    /**
     * add Bower tab
     *
     * @param tabs          - tabs list
     * @param addonsManager - add on managers
     */
    private void addBowerTab(List<IArtifactInfo> tabs, AddonsManager addonsManager, Properties properties) {
        boolean isBowerFileTypeAndSupported = isRepoSupportType(RepoType.Bower)
                && isBowerFile(addonsManager, properties) && addonsManager.isAddonSupported(AddonType.BOWER);
        if (isBowerFileTypeAndSupported) {
            tabs.add(new BaseArtifactInfo("BowerInfo"));
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    private boolean isXmlFile() {
        return NamingUtils.isXml(fileInfo.getName());
    }

    private boolean isIvyFile() {
        return IvyNaming.isIvyFileName(fileInfo.getName());
    }

    private boolean isPomFile() {
        return MavenNaming.isPom((fileInfo.getName()));
    }

    private boolean isJnlpFile() {
        MimeType mimeType = NamingUtils.getMimeType((fileInfo.getName()));
        return "application/x-java-jnlp-file".equalsIgnoreCase(mimeType.getType());
    }

    private boolean isRpmFile() {
        return fileInfo.getName().endsWith(".rpm");
    }

    private boolean isNuPkgFile() {
        MimeType mimeType = NamingUtils.getMimeType((fileInfo.getName()));
        return "application/x-nupkg".equalsIgnoreCase(mimeType.getType());
    }

    private boolean isGemFile() {
        MimeType mimeType = NamingUtils.getMimeType((fileInfo.getName()));
        return "application/x-rubygems".equalsIgnoreCase(mimeType.getType());
    }

    private boolean isBowerFile(AddonsManager addonsManager, Properties properties ){
        return addonsManager.addonByType(BowerAddon.class).isBowerFile(fileInfo.getName())
                && properties != null && properties.containsKey("bower.name");
    }

    private boolean isNpmFile(Properties properties) {
        return fileInfo.getName().endsWith(".tgz") && properties != null && properties.containsKey("npm.name");
    }

    private boolean isPypiFile(Properties properties) {
        AddonsManager addonsProvider = ContextHelper.get().beanForType(AddonsManager.class);
        PypiAddon pypiWebAddon = addonsProvider.addonByType(PypiAddon.class);
        return pypiWebAddon.isPypiFile((fileInfo)) && properties != null && properties.containsKey("pypi.name");
    }

    @Override
    public List<? extends RestTreeNode> getChildren(AuthorizationService authService, boolean isCompact,
            ArtifactoryRestRequest request) {
        List<INode>  childNodeList = new ArrayList<>();
        childNodeList.add(this);
        return childNodeList;
    }


    /**
     * add download actions
     *
     * @param actions - actions list
     */
    private void addDownloadAction(List<IAction> actions) {
        actions.add(new BaseArtifact("Download"));
    }

    /**
     *  add view action
     * @param actions - action list
     */
    private void addViewAction(List<IAction> actions) {
        if (NamingUtils.isViewable(getPath()) || "class".equals(PathUtils.getExtension(getPath()))) {
            actions.add(new BaseArtifact("View"));
        }
    }


    /**
     * update additional file info data
     */
    private void updateFileInfo() {
        if (fileInfo == null){
            super.setRepoPath(InternalRepoPathFactory.create(getRepoKey(), getPath()));
            fileInfo = getRepoService().getFileInfo(getRepoPath());
        }
    }


    @Override
    public void updateNodeData() {
        if (isLocal()) {
            if (fileInfo != null) {
                mimeType = NamingUtils.getMimeType(fileInfo.getRelPath()).getType();
            } else {
                mimeType = NamingUtils.getMimeType(this.getPath()).getType();
            }
        }
    }

    protected void updateHasChild(boolean hasChild){
        super.setHasChild(hasChild);
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }

    @Override
    public List<RestModel> fetchItemTypeData(AuthorizationService authService, boolean isCompact, Properties props,
            ArtifactoryRestRequest request) {
        Collection<? extends RestTreeNode> items = getChildren(authService, isCompact, request);
        List<RestModel> treeModel = new ArrayList<>();
        items.forEach(item -> {
            ((INode) item).populateActions(authService);
            // populate tabs
            ((INode) item).populateTabs(authService);
            // update additional data
            ((INode) item).updateNodeData();
            treeModel.add(item);
        });
        return treeModel;
    }
}
