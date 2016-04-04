package org.artifactory.post.providers.features;

import org.artifactory.api.callhome.FeatureGroup;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class represent the repositories feature group of the CallHome feature
 *
 * @author Shay Bagants
 */
@Component
public class RepositoriesFeature implements CallHomeFeature {

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private InternalRepositoryService repoService;

    @Override
    public FeatureGroup getFeature() {

        FeatureGroup repositoriesFeature = new FeatureGroup("repositories");
        FeatureGroup localRepositoriesFeature = new FeatureGroup("local repositories");
        FeatureGroup remoteRepositoriesFeature = new FeatureGroup("remote repositories");
        List<RealRepo> localAndRemoteRepositories = repoService.getLocalAndRemoteRepositories();
        localAndRemoteRepositories.stream().forEach(rr -> {
            if(rr.isLocal()) {
                addLocalRepoFeatures(localRepositoriesFeature, rr);

            } else {
                addRemoteRepoFeatures(remoteRepositoriesFeature, rr);

            }
        });

        long localCount = localAndRemoteRepositories.parallelStream().filter(r -> r.isLocal()).count();
        localRepositoriesFeature.addFeatureAttribute("number_of_repositories", localCount);
        remoteRepositoriesFeature.addFeatureAttribute("number_of_repositories", localAndRemoteRepositories.size()-localCount);

        repositoriesFeature.addFeature(localRepositoriesFeature);
        repositoriesFeature.addFeature(remoteRepositoriesFeature);

        // virtual repos
        FeatureGroup virtualRepositoriesFeature = new FeatureGroup("virtual repositories");
        List<VirtualRepo> virtualRepositories = repoService.getVirtualRepositories();
        virtualRepositoriesFeature.addFeatureAttribute("number_of_repositories", getVirtualReposSize(virtualRepositories));
        addVirtualRepoFeatures(virtualRepositoriesFeature, virtualRepositories);
        repositoriesFeature.addFeature(virtualRepositoriesFeature);

        return repositoriesFeature;
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     *
     * @param localRepositoriesFeature
     * @param localRepo
     */
    private void addLocalRepoFeatures(FeatureGroup localRepositoriesFeature, final RealRepo localRepo) {
        // local repos
        localRepositoriesFeature.addFeature(new FeatureGroup(localRepo.getKey()) {{
            addFeatureAttribute("package_type", localRepo.getDescriptor().getType().name());
            addFeatureAttribute("repository_layout",
                    localRepo.getDescriptor().getRepoLayout().getName());

            LocalReplicationDescriptor localReplication =
                    configService.getDescriptor().getEnabledLocalReplication(localRepo.getKey());

            if (localReplication != null && localReplication.isEnabled()) {
                List<LocalReplicationDescriptor> repls =
                        configService.getDescriptor().getMultiLocalReplications(localRepo.getKey());
                addFeatureAttribute("push_replication", (repls == null || repls.size() == 0 ? false :
                        repls.size() > 1 ? "multi" : "true"));
                addFeatureAttribute("event_replication", localReplication.isEnableEventReplication());
                addFeatureAttribute("sync_properties", localReplication.isSyncProperties());
                addFeatureAttribute("sync_deleted", localReplication.isSyncDeletes());
            } else if (localReplication == null) {
                addFeatureAttribute("push_replication", false);
                addFeatureAttribute("event_replication", false);
                addFeatureAttribute("sync_deleted", false);
            }
        }});
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     *
     * @param remoteRepositoriesFeature
     * @param remoteRepo
     */
    private void addRemoteRepoFeatures(FeatureGroup remoteRepositoriesFeature, final RealRepo remoteRepo) {
        // remote repos
        remoteRepositoriesFeature.addFeature(new FeatureGroup(remoteRepo.getKey()) {{
            addFeatureAttribute("package_type", remoteRepo.getDescriptor().getType().name());
            addFeatureAttribute("repository_layout",
                    remoteRepo.getDescriptor().getRepoLayout().getName());

            RemoteReplicationDescriptor remoteReplicationDescriptor =
                    configService.getDescriptor().getRemoteReplication(remoteRepo.getKey());

            if (remoteReplicationDescriptor != null) {
                addFeatureAttribute("pull_replication", remoteReplicationDescriptor.isEnabled());
                if(remoteReplicationDescriptor.isEnabled()) {
                    addFeatureAttribute("pull_replication_url",
                            ((RemoteRepoDescriptor)remoteRepo.getDescriptor()).getUrl());
                }
            } else {
                addFeatureAttribute("pull_replication", false);
            }
        }});
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     *
     * @param virtualRepositoriesFeature
     * @param virtualRepositories
     */
    private void addVirtualRepoFeatures(FeatureGroup virtualRepositoriesFeature,
                                        List<VirtualRepo> virtualRepositories) {
        virtualRepositories.stream()
                .filter(vr -> !vr.getKey().equals("repo"))
                .forEach(vr -> {
                    virtualRepositoriesFeature.addFeature(new FeatureGroup(vr.getKey()) {{
                        addFeatureAttribute("number_of_included_repositories",
                                vr.getResolvedLocalRepos().size() + vr.getResolvedRemoteRepos().size());
                        addFeatureAttribute("package_type", vr.getDescriptor().getType().name());
                        if (vr.getDescriptor().getRepoLayout() != null) {
                            addFeatureAttribute("repository_layout", vr.getDescriptor().getRepoLayout().getName());
                        }
                        if (vr.getDescriptor().getDefaultDeploymentRepo() != null)
                            addFeatureAttribute("configured_local_deployment",
                                    vr.getDescriptor().getDefaultDeploymentRepo().getKey());
                    }});
                });
    }

    /**
     * Calculates sizeof List<VirtualRepo>
     *
     * @param virtualRepositories
     * @return size
     */
    private int getVirtualReposSize(List<VirtualRepo> virtualRepositories) {
        return virtualRepositories.stream().filter(vr -> vr.getKey().equals("repo")).count() == 1 ?
                virtualRepositories.size() -1 : virtualRepositories.size();
    }
}
