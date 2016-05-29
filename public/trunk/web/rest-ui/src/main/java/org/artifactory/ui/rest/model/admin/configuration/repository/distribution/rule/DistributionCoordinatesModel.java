package org.artifactory.ui.rest.model.admin.configuration.repository.distribution.rule;

import org.artifactory.descriptor.repo.distribution.DistributionCoordinates;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.util.distribution.DistributionConstants.PATH_TOKEN;


/**
 * @author Dan Feldman
 */
public class DistributionCoordinatesModel implements RestModel {

    private String repo;
    private String pkg;
    private String version;
    private String path = PATH_TOKEN;

    public DistributionCoordinatesModel() {

    }

    public DistributionCoordinatesModel(DistributionCoordinates coordinates) {
        this.repo = coordinates.getRepo();
        this.pkg = coordinates.getPkg();
        this.version = coordinates.getVersion();
        this.path = coordinates.getPath();
    }

    public DistributionCoordinates toCoordinates() {
        return new DistributionCoordinates(this.repo, this.pkg, this.version, this.path);
    }


    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
