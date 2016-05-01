package org.artifactory.maven;

import org.artifactory.api.maven.MavenMetadataService;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.RepoLayoutUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A service for calculating maven metadata.
 *
 * @author Yossi Shaul
 */
@Service
public class MavenMetadataServiceImpl implements MavenMetadataService {
    private static final Logger log = LoggerFactory.getLogger(MavenMetadataServiceImpl.class);
    // work queue by repo path that requires maven metadata calculation
    private final WorkQueue<MavenMetadataWorkItem> mavenMetadataWorkQueue =
            new WorkQueue<>("Maven Metadata", (w) -> calculateMavenMetadata(w.repoPath, w.recursive));
    // work queue by repository keys that requires maven metadata plugins calculation
    private final WorkQueue<String> pluginsMDWorkQueue =
            new WorkQueue<>("Maven Plugin Metadata", new MavenPluginMetadataWorkExecutor());
    @Autowired
    private InternalRepositoryService repoService;

    @Override
    public void calculateMavenMetadataAsync(RepoPath folderPath, boolean recursive) {
        mavenMetadataWorkQueue.offerWork(new MavenMetadataWorkItem(folderPath, recursive));
    }

    @Override
    public void calculateMavenMetadataAsyncNonRecursive(Set<RepoPath> folderPaths) {
        for (RepoPath folderPath : folderPaths) {
            mavenMetadataWorkQueue.offerWork(new MavenMetadataWorkItem(folderPath, false));
        }
    }

    @Override
    public void calculateMavenMetadata(RepoPath baseFolderPath, boolean recursive) {
        if (baseFolderPath == null) {
            log.debug("Couldn't find repo for null repo path.");
            return;
        }
        LocalRepo localRepo = repoService.localRepositoryByKey(baseFolderPath.getRepoKey());
        if (localRepo == null) {
            log.debug("Couldn't find local non-cache repository for path '{}'.", baseFolderPath);
            return;
        }
        log.debug("Calculate maven metadata on {}", baseFolderPath);
        RepoLayout repoLayout = localRepo.getDescriptor().getRepoLayout();
        RepoType type = localRepo.getDescriptor().getType();
        // Do not calculate maven metadata if type == null or type doesn't belong to the maven group (Maven, Ivy, Gradle) or repoLayout not equals MAVEN_2_DEFAULT
        if (type != null && !(type.isMavenGroup() || RepoLayoutUtils.MAVEN_2_DEFAULT.equals(repoLayout))) {
            log.debug("Skipping maven metadata calculation since repoType '{}' doesn't belong to " +
                    "neither Maven, Ivy, Gradle repositories types.", baseFolderPath.getRepoKey());
            return;
        }

        new MavenMetadataCalculator(baseFolderPath, recursive).calculate();
        // Calculate maven plugins metadata asynchronously
        getTransactionalMe().calculateMavenPluginsMetadataAsync(localRepo.getKey());
    }

    // get all folders marked for maven metadata calculation and execute the metadata calculation
    @Override
    public void calculateMavenPluginsMetadataAsync(String repoKey) {
        pluginsMDWorkQueue.offerWork(repoKey);
    }

    private LocalRepo localRepositoryByKeyFailIfNull(RepoPath localRepoPath) {
        LocalRepo localRepo = repoService.localRepositoryByKey(localRepoPath.getRepoKey());
        if (localRepo == null) {
            throw new IllegalArgumentException("Couldn't find local non-cache repository for path " + localRepoPath);
        }
        return localRepo;
    }

    private static MavenMetadataService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(MavenMetadataService.class);
    }

    private class MavenPluginMetadataWorkExecutor implements Consumer<String> {
        @Override
        public void accept(String repoToCalculate) {
            try {
                LocalRepo localRepo = localRepositoryByKeyFailIfNull(
                        InternalRepoPathFactory.repoRootPath(repoToCalculate));
                new MavenPluginsMetadataCalculator().calculate(localRepo);
            } catch (Exception e) {
                log.error("Failed to calculate plugin maven metadata on repo '{}'", repoToCalculate, e);
            }
        }
    }

    private static class MavenMetadataWorkItem {
        private final RepoPath repoPath;
        private final boolean recursive;

        public MavenMetadataWorkItem(RepoPath repoPath, boolean recursive) {
            this.repoPath = repoPath;
            this.recursive = recursive;
        }

        @Override
        public String toString() {
            return "MavenMetadataWorkItem{" +
                    "repoPath=" + repoPath +
                    ", recursive=" + recursive +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MavenMetadataWorkItem that = (MavenMetadataWorkItem) o;
            if (recursive != that.recursive) return false;
            return repoPath.equals(that.repoPath);

        }

        @Override
        public int hashCode() {
            int result = repoPath.hashCode();
            result = 31 * result + (recursive ? 1 : 0);
            return result;
        }
    }
}
