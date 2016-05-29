package org.artifactory.ui.rest.service.admin.security.permissions;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AclService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AceInfo;
import org.artifactory.security.AclInfo;
import org.artifactory.security.ArtifactoryPermission;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.ui.rest.model.admin.security.permissions.PermissionTargetModel;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.permission.EffectivePermission;
import org.artifactory.ui.rest.model.utils.repositories.RepoKeyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Chen keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetPermissionsTargetService implements RestService {
    @Autowired
    AclService aclService;
    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String id = request.getPathParamByKey("name");
        List<RepoKeyType> repoList = getAllRepositoriesData();
        if (StringUtils.isEmpty(id)) {
            fetchAllTargetPermission(response, repoList);
        }
        else {
            fetchSingleTargetPermission(response, id, repoList);
        }
    }

    /**
     * fetch Single target permission
     * @param artifactoryResponse - encapsulate data related to response
     * @param id - permission id
     * @param repoList - repository list
     */
    private void fetchSingleTargetPermission(RestResponse artifactoryResponse, String id, List<RepoKeyType> repoList) {
        AclInfo aclInfo = aclService.getAcl(id);
        // populate permission model data
        PermissionTargetInfo permission = aclInfo.getPermissionTarget();
        if (!aclService.canManage(permission)){
            artifactoryResponse.responseCode(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        PermissionTargetModel permissionTarget = new PermissionTargetModel(permission);
        // filter included repo key from available repo keys
        updateSelectedAndAvailableRepo(repoList, permission, permissionTarget);
        // get groups
        aclInfo.getAces().stream().filter(AceInfo::isGroup).forEach(aceInfo ->
                permissionTarget.getGroups().add(new EffectivePermission(aceInfo)));
        // get users
        aclInfo.getAces().stream().filter(ace -> !ace.isGroup()).forEach(aceInfo ->
                permissionTarget.getUsers().add(new EffectivePermission(aceInfo)));
        artifactoryResponse.iModel(permissionTarget);
    }

    /**
     * fetch All target permission
     * @param artifactoryResponse - encapsulate data related to response
     * @param repoList - repository list
     */
    private void fetchAllTargetPermission(RestResponse artifactoryResponse, List<RepoKeyType> repoList) {
        List<PermissionTargetInfo> permissionTargets = aclService.getPermissionTargets(ArtifactoryPermission.MANAGE);
        List<PermissionTargetModel> permissionTargetModels = new ArrayList<>();
        permissionTargets.forEach(permissionTargetInfo -> {
            AclInfo aclInfos = aclService.getAcl(permissionTargetInfo.getName());
            // populate permission model data
            PermissionTargetModel permissionTarget = new PermissionTargetModel(permissionTargetInfo);
            // filter included repo key from available repo keys
            updateSelectedAndAvailableRepo(repoList, permissionTargetInfo, permissionTarget);
            // get groups
            aclInfos.getAces().stream().filter(AceInfo::isGroup).forEach(aceInfo ->
                    permissionTarget.getGroups().add(new EffectivePermission(aceInfo)));
            // get users
            aclInfos.getAces().stream().filter(ace -> !ace.isGroup()).forEach(aceInfo ->
                    permissionTarget.getUsers().add(new EffectivePermission(aceInfo)));
            permissionTargetModels.add(permissionTarget);
        });
        artifactoryResponse.iModelList(permissionTargetModels);
    }

    /**
     * update selected and available repo
     * @param repoList - repository list
     * @param permissionTargetInfo - permission target info
     * @param permissionTarget - permission target
     */
    private void updateSelectedAndAvailableRepo(List<RepoKeyType> repoList, PermissionTargetInfo permissionTargetInfo,
                                                PermissionTargetModel permissionTarget) {
        List<RepoKeyType> repoKeys = permissionTarget.getRepoKeys();
        List<String> tempRepoKeysList = new ArrayList<>();
        // selected include remote and local repositories
        if (permissionTargetInfo.getRepoKeys().contains("ANY")){
            // update select all
            updateSelectAll(repoList, permissionTarget, repoKeys);
        }else {
            updateSpecifiedRepos(repoList, permissionTargetInfo, permissionTarget, repoKeys, tempRepoKeysList);

        }
    }

    /**
     * update repositories for specified list
     *
     * @param repoList             - repo list
     * @param permissionTargetInfo - permission target info
     * @param permissionTarget     - permission targer
     * @param repoKeys             - repo keys list
     * @param tempRepoKeysList     - temp repo list
     */
    private void updateSpecifiedRepos(List<RepoKeyType> repoList, PermissionTargetInfo permissionTargetInfo,
            PermissionTargetModel permissionTarget, List<RepoKeyType> repoKeys, List<String> tempRepoKeysList) {


        permissionTargetInfo.getRepoKeys()
                .forEach(repoKey -> {
                    boolean anyLocal = repoKey.equals("ANY LOCAL");
                    if (anyLocal) {
                        updateSelectedAnyLocal(repoList, permissionTarget, repoKeys, tempRepoKeysList);
                    }
                    boolean anyRemote = repoKey.equals("ANY REMOTE");
                    if (anyRemote) {
                        updateSelectedAnyRemote(repoList, permissionTarget, repoKeys, tempRepoKeysList);
                    }
                    if (!anyLocal && !anyRemote) {
                        if (repoKey.endsWith("-cache")) {
                            tempRepoKeysList.add(repoKey.substring(0, repoKey.length() - 6));
                        } else {
                            tempRepoKeysList.add(repoKey);
                        }
                    }
                });
        // update available repo keys
        updateAvailableRepoKeys(repoList, permissionTarget, tempRepoKeysList);
    }

    /**
     * update permission Available repo keys list
     *
     * @param repoList         - all repo list keys
     * @param permissionTarget - target permission selected repo keys
     * @param tempRepoKeysList - temp repo keys list
     */
    private void updateAvailableRepoKeys(List<RepoKeyType> repoList, PermissionTargetModel permissionTarget,
            List<String> tempRepoKeysList) {
        repoList.stream().filter((RepoKeyType key) -> !tempRepoKeysList.contains(key.getRepoKey())).
                forEach(key -> permissionTarget.getAvailableRepoKeys().add(new RepoKeyType(key.getType(),key.getRepoKey())));
            }

    /**
     * update selected list for ANY repo keys
     *
     * @param repoList         - full repo list
     * @param permissionTarget - permission target include selected repo keys
     * @param repoKeys         - selected repo list
     */
    private void updateSelectAll(List<RepoKeyType> repoList, PermissionTargetModel permissionTarget,
            List<RepoKeyType> repoKeys) {
        repoList.forEach(key -> repoKeys.add(new RepoKeyType(key.getType(),key.getRepoKey())));
        permissionTarget.setAnyLocal(true);
        permissionTarget.setAnyRemote(true);
        permissionTarget.setAnyDistribution(true);
    }


    /**
     * update selected any local
     * @param repoList - all repo list
     * @param permissionTarget - permission target model
     * @param repoKeys - repo keys
     */
    private void updateSelectedAnyLocal(List<RepoKeyType> repoList,
            PermissionTargetModel permissionTarget, List<RepoKeyType> repoKeys, List<String> tempList) {
        repoKeys.forEach(key -> tempList.add(key.getRepoKey()));
        repoList.stream().forEach(key -> {
            if (key.getType().equals("local")) {
                repoKeys.add(new RepoKeyType(key.getType(), key.getRepoKey()));
                tempList.add(key.getRepoKey());
            }
        });
        permissionTarget.setAnyLocal(true);
    }

    /**
     * update selected any remote
     * @param repoList - all repo list
     * @param permissionTarget - permission target model
     * @param repoKeys - repo keys
     */
    private void updateSelectedAnyRemote(List<RepoKeyType> repoList,
            PermissionTargetModel permissionTarget, List<RepoKeyType> repoKeys, List<String> tempList) {
        repoKeys.forEach(key -> tempList.add(key.getRepoKey()));
        repoList.stream().filter(key -> key.getType().equals("remote")).forEach(key -> {
            if (key.getType().equals("remote")) {
                repoKeys.add(new RepoKeyType(key.getType(), key.getRepoKey()));
            }
            tempList.add(key.getRepoKey());
        });
        permissionTarget.setAnyRemote(true);
    }

    /**
     * return remote and local repository data
     *
     * @return list of repositories repo keys
     */
    private List<RepoKeyType> getAllRepositoriesData() {
        List<RepoKeyType> repos = new ArrayList<>();
        Map<String, LocalRepoDescriptor> localRepoDescriptorMap = centralConfigService.getDescriptor().getLocalRepositoriesMap();
        localRepoDescriptorMap.keySet().forEach(key -> repos.add(new RepoKeyType("local", key)));
        Map<String, RemoteRepoDescriptor> remoteRepoDescriptorMap = centralConfigService.getDescriptor().getRemoteRepositoriesMap();
        remoteRepoDescriptorMap.keySet().forEach(key -> repos.add(new RepoKeyType("remote", key)));
        Map<String, DistributionRepoDescriptor> distRepoDescriptorMap = centralConfigService.getDescriptor().getDistributionRepositoriesMap();
        distRepoDescriptorMap.keySet().forEach(key -> repos.add(new RepoKeyType("distribution", key)));
        return repos;
    }
}
