package org.artifactory.api.bintray.distribution;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * A Distribution denotes an actions which distributes either a build or a list of files to Bintray using the specified
 * target repo's rules.
 *
 * @author Dan Feldman
 */
public class Distribution {

    private Boolean publish;
    private Boolean overrideExistingFiles;
    private List<String> sourceRepos;                                 //Used only when distributing build.
    private List<String> packagesRepoPaths = new ArrayList<>();       //Used only when distributing packages.
    private String targetRepo;
    private String gpgPassphrase;
    private boolean async = false;
    private boolean dryRun = false;

    public Boolean isPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }

    public Boolean isOverrideExistingFiles() {
        return overrideExistingFiles != null && overrideExistingFiles;
    }

    public void setOverrideExistingFiles(Boolean overrideExistingFiles) {
        this.overrideExistingFiles = overrideExistingFiles;
    }

    public List<String> getSourceRepos() {
        return sourceRepos;
    }

    public void setSourceRepos(List<String> sourceRepos) {
        this.sourceRepos = sourceRepos;
    }

    public List<String> getPackagesRepoPaths() {
        return packagesRepoPaths;
    }

    public void setPackagesRepoPaths(List<String> packagesRepoPaths) {
        this.packagesRepoPaths = packagesRepoPaths;
    }

    @JsonIgnore
    public void addPath(String fullRepoPath) {
        packagesRepoPaths.add(fullRepoPath);
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
    }

    public String getGpgPassphrase() {
        return gpgPassphrase;
    }

    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
