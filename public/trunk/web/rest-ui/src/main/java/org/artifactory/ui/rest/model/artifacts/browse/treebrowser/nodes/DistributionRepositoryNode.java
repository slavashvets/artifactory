package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.md.Properties;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.model.artifact.BaseArtifact;
import org.artifactory.rest.common.model.artifact.IAction;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.RefreshArtifact;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Dan Feldman
 */
@JsonTypeName("distributionRepository")
@JsonIgnoreProperties("repoPath")
public class DistributionRepositoryNode extends BaseNode {

    DistributionRepositoryNode(RepoBaseDescriptor repo) {
        super(InternalRepoPathFactory.create(repo.getKey(), ""));
        this.repoType = "distribution";
        RepoPath repoPath = InternalRepoPathFactory.create(repo.getKey(), "");
        super.setText(repoPath.getRepoKey());
        setLocal(true);
    }

    public String getType() {
        return "repository";
    }

    @Override
    public Collection<? extends RestTreeNode> getChildren(AuthorizationService authService, boolean isCompact,
            ArtifactoryRestRequest request) {
        List<INode> childNodeList = new ArrayList<>();
        childNodeList.add(this);
        return childNodeList;
    }

    @Override
    public void populateActions(AuthorizationService authService) {
        List<IAction> actions = new ArrayList<>();
        // update repo path and auth data
        RepoPath repoPath = getRepoPath();
        boolean canDelete = authService.canDelete(repoPath);
        boolean canRead = authService.canRead(repoPath);
        addRefreshAction(actions);
        addWatchAction(authService, actions, canRead);
        addDeleteAction(actions, canDelete);
        setActions(actions);
    }

    @Override
    protected RepoPath fetchRepoPath() {
        return super.getRepoPath();
    }

    /**
     * add refresh action
     *
     * @param actions - action list
     */
    private void addRefreshAction(List<IAction> actions) {
        actions.add(new RefreshArtifact("Refresh"));
    }

    @Override
    protected void addDeleteAction(List<IAction> actions, boolean canDelete) {
        if (canDelete) {
            actions.add(new BaseArtifact("DeleteContent"));
        }
    }

    @Override
    public Collection<? extends RestModel> fetchItemTypeData(AuthorizationService authService, boolean isCompact,
            Properties props, ArtifactoryRestRequest request) {
        return getRepoOrFolderChildren(authService, isCompact, request);
    }

    /**
     * get repository or folder children
     *
     * @param authService - authorization service
     * @param isCompact   - is compacted
     * @param request
     * @return
     */
    private Collection<? extends RestModel> getRepoOrFolderChildren(AuthorizationService authService, boolean isCompact,
            ArtifactoryRestRequest request) {
        Collection<? extends RestTreeNode> items = getChildren(authService, isCompact, request);
        List<RestModel> treeModel = new ArrayList<>();
        items.forEach(item -> {
            // update additional data
            ((INode) item).updateNodeData();
            treeModel.add(item);
        });
        return treeModel;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
