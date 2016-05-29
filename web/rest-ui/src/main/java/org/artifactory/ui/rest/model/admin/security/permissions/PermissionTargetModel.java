package org.artifactory.ui.rest.model.admin.security.permissions;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.EffectivePermission;
import org.artifactory.ui.rest.model.utils.repositories.RepoKeyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Chen Keinan
 */
public class PermissionTargetModel extends BaseModel {
    private String name;
    private List<RepoKeyType> repoKeys = new ArrayList<>();
    private List<String> includes;
    private List<String> exclude;
    private String includePattern;
    private String excludePattern;
    private List<RepoKeyType> availableRepoKeys = new ArrayList<>();
    private List<EffectivePermission> groups = new ArrayList<>();
    private List<EffectivePermission> users = new ArrayList<>();
    private boolean anyRemote;
    private boolean anyLocal;
    private boolean anyDistribution;
    private String repoKeysView;
    private String userView;
    private String groupsView;
    private transient int numOfDistRepos = 0;

    public PermissionTargetModel() {
    }

    public PermissionTargetModel(PermissionTargetInfo permissionTargetInfo) {
        this.name = permissionTargetInfo.getName();
        updateRepoKeysData(permissionTargetInfo);
        this.includes = permissionTargetInfo.getIncludes();
        this.exclude = permissionTargetInfo.getExcludes();
        this.includePattern = permissionTargetInfo.getIncludesPattern();
        this.excludePattern = permissionTargetInfo.getExcludesPattern();
    }

    /**
     * update repo  key value data
     * @param permissionTargetInfo - permission target info
     */
    private void updateRepoKeysData(PermissionTargetInfo permissionTargetInfo) {
        updateRepoKeyView(permissionTargetInfo);
        Map<String, DistributionRepoDescriptor> distRepos =
                ContextHelper.get().getCentralConfig().getDescriptor().getDistributionRepositoriesMap();
        permissionTargetInfo.getRepoKeys().forEach(repoKey-> {
            if (repoKey.equals("ANY REMOTE")) {
                anyRemote = true;
            }
            if (repoKey.equals("ANY LOCAL")) {
                anyLocal = true;
            }
            if (repoKey.equals("ANY")) {
                anyLocal = true;
                anyRemote = true;
                anyDistribution = true;
            }
            if (repoKey.endsWith("-cache")) {
                this.repoKeys.add(new RepoKeyType("remote", repoKey.replace("-cache", "")));
            }
            if (!repoKey.endsWith("-cache") && !repoKey.equals("ANY REMOTE") &&
                    !repoKey.equals("ANY LOCAL") && !repoKey.equals("ANY")) {
                if (distRepos.containsKey(repoKey)) {
                    this.repoKeys.add(new RepoKeyType("distribution", repoKey));
                    numOfDistRepos++;
                } else {
                    this.repoKeys.add(new RepoKeyType("local", repoKey));
                }
            }
        });
        anyDistribution = distRepos.size() == numOfDistRepos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RepoKeyType> getRepoKeys() {
        return repoKeys;
    }

    public void setRepoKeys(List<RepoKeyType> repoKeys) {
        this.repoKeys = repoKeys;
    }

    public List<RepoKeyType> getAvailableRepoKeys() {
        return availableRepoKeys;
    }

    public void setAvailableRepoKeys(List<RepoKeyType> availableRepoKeys) {
        this.availableRepoKeys = availableRepoKeys;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    public String getIncludePattern() {
        return includePattern;
    }

    public void setIncludePattern(String includePattern) {
        this.includePattern = includePattern;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    /**
     * update repositories column data for grid
     *
     * @param permissionTargetInfo - permission target data
     */
    void updateRepoKeyView(PermissionTargetInfo permissionTargetInfo) {
        if (permissionTargetInfo.getRepoKeys() != null && !permissionTargetInfo.getRepoKeys().isEmpty()) {
            StringBuilder keysBuilder = new StringBuilder();
            permissionTargetInfo.getRepoKeys().forEach(repoKey -> {
                if (repoKey != null) {
                    keysBuilder.append(repoKey);
                    keysBuilder.append(", ");
                }
            });
            repoKeysView = keysBuilder.toString();
        }
    }

    public String getRepoKeysView() {
        if (repoKeys != null && !repoKeys.isEmpty()) {
            return repoKeys.size() + " | " + repoKeysView.substring(0, repoKeysView.length() - 2).toString();
        } else {
            return "";
        }
    }

    public String getUserView() {
        if (users != null && !users.isEmpty()) {
            StringBuilder keysBuilder = new StringBuilder();
            users.forEach(user -> {
                keysBuilder.append(user.getPrincipal());
                keysBuilder.append(", ");
            });
            String keyChain = keysBuilder.toString();
            return users.size() + " | " + keyChain.substring(0, keyChain.length() - 2).toString();
        } else {
            return "";
        }
    }

    public String getGroupsView() {
        if (groups != null && !groups.isEmpty()) {
            StringBuilder keysBuilder = new StringBuilder();
            groups.forEach(group -> {
                keysBuilder.append(group.getPrincipal());
                keysBuilder.append(", ");
            });
            String keyChain = keysBuilder.toString();
            return groups.size() + " | " + keyChain.substring(0, keyChain.length() - 2).toString();
        } else {
            return "";
        }
    }

    public List<EffectivePermission> getGroups() {
        return groups;
    }

    public void setGroups(List<EffectivePermission> groups) {
        this.groups = groups;
    }

    public List<EffectivePermission> getUsers() {
        return users;
    }

    public void setUsers(List<EffectivePermission> users) {
        this.users = users;
    }

    public boolean isAnyRemote() {
        return anyRemote;
    }

    public void setAnyRemote(boolean anyRemote) {
        this.anyRemote = anyRemote;
    }

    public boolean isAnyLocal() {
        return anyLocal;
    }

    public void setAnyLocal(boolean anyLocal) {
        this.anyLocal = anyLocal;
    }

    public boolean isAnyDistribution() {
        return anyDistribution;
    }

    public void setAnyDistribution(boolean anyDistribution) {
        this.anyDistribution = anyDistribution;
    }

    public void setRepoKeysView(String repoKeysView) {
        this.repoKeysView = repoKeysView;
    }

    public void setUserView(String userView) {
        this.userView = userView;
    }

    public void setGroupsView(String groupsView) {
        this.groupsView = groupsView;
    }
}
