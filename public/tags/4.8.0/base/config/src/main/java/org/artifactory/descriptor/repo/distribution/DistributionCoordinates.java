package org.artifactory.descriptor.repo.distribution;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.artifactory.util.distribution.DistributionConstants.PATH_TOKEN;

/**
 * Coordinates that are used when distributing artifacts to Bintray to specify exactly where the artifact goes.
 * Each coordinate is a string that can be a 'hardcoded' user input, the set of distribution rule tokens available to
 * the rule or a combination of both.
 *
 * @author Dan Feldman
 */
@XmlType(name = "DistributionCoordinatesType", propOrder = {"repo", "pkg", "version", "path"}, namespace = Descriptor.NS)
public class DistributionCoordinates implements Descriptor {

    @XmlElement(required = true)
    protected String repo;

    @XmlElement(required = true)
    protected String pkg;

    @XmlElement(required = true)
    protected String version;

    @XmlElement(required = true)
    protected String path = PATH_TOKEN;

    public DistributionCoordinates() {

    }

    public DistributionCoordinates(String repo, String pkg, String version, String path) {
        this.repo = repo;
        this.pkg = pkg;
        this.version = version;
        this.path = path;
    }

    public DistributionCoordinates(DistributionCoordinates copy) {
        this.repo = copy.repo;
        this.pkg = copy.pkg;
        this.version = copy.version;
        this.path = copy.path;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DistributionCoordinates)) return false;

        DistributionCoordinates that = (DistributionCoordinates) o;

        if (getRepo() != null ? !getRepo().equals(that.getRepo()) : that.getRepo() != null) return false;
        if (getPkg() != null ? !getPkg().equals(that.getPkg()) : that.getPkg() != null) return false;
        if (getVersion() != null ? !getVersion().equals(that.getVersion()) : that.getVersion() != null) return false;
        return getPath() != null ? getPath().equals(that.getPath()) : that.getPath() == null;
    }

    @Override
    public int hashCode() {
        int result = getRepo() != null ? getRepo().hashCode() : 0;
        result = 31 * result + (getPkg() != null ? getPkg().hashCode() : 0);
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        result = 31 * result + (getPath() != null ? getPath().hashCode() : 0);
        return result;
    }
}
