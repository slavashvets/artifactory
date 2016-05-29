package org.artifactory.sapi.interceptor;

import org.artifactory.repo.RepoPath;

/**
 * A context to pass when performing item deletion.
 *
 * @author Yossi Shaul
 */
public class DeleteContext {

    private final RepoPath repoPath;
    private boolean calculateMavenMetadata;
    private boolean triggeredByMove;

    public DeleteContext(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    /**
     * @return The original repo path send to delete.
     */
    public RepoPath getRepoPath() {
        return repoPath;
    }

    public boolean isCalculateMavenMetadata() {
        return calculateMavenMetadata;
    }

    public DeleteContext calculateMavenMetadata() {
        return calculateMavenMetadata(true);
    }

    public DeleteContext calculateMavenMetadata(boolean calculateMavenMetadata) {
        this.calculateMavenMetadata = calculateMavenMetadata;
        return this;
    }

    public boolean isTriggeredByMove() {
        return triggeredByMove;
    }

    public DeleteContext triggeredByMove() {
        triggeredByMove = true;
        return this;
    }
}
