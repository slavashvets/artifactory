/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.descriptor.repo.distribution.rule;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.distribution.DistributionCoordinates;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

/**
 * A Distribution rule used to map an artifacts to its distribution coordinates in Bintray
 *
 * @author Dan Feldman
 */
@XmlType(name = "DistributionRuleType", propOrder = {"name", "type", "repoFilter", "pathFilter", "distributionCoordinates"},
        namespace = Descriptor.NS)
public class DistributionRule implements Descriptor {

    @XmlID
    @XmlElement(required = true)
    protected String name;

    @XmlElement(required = true)
    protected RepoType type;

    @XmlElement(required = false)
    protected String repoFilter;

    @XmlElement(required = false)
    protected String pathFilter;

    @XmlElement(required = true)
    protected DistributionCoordinates distributionCoordinates = new DistributionCoordinates();

    public DistributionRule() {
    }

    public DistributionRule(DistributionRule copy) {
        this.name = copy.name;
        this.type = copy.type;
        this.repoFilter = copy.repoFilter;
        this.pathFilter = copy.pathFilter;
        this.distributionCoordinates = copy.distributionCoordinates;
    }

    public DistributionRule(String name, RepoType type, String repoFilter, String pathFilter, DistributionCoordinates coordinates) {
        this.name = name;
        this.type = type;
        this.repoFilter = repoFilter;
        this.pathFilter = pathFilter;
        this.distributionCoordinates = coordinates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RepoType getType() {
        return type;
    }

    public void setType(RepoType type) {
        this.type = type;
    }

    public String getRepoFilter() {
        return repoFilter;
    }

    public void setRepoFilter(String repoFilter) {
        this.repoFilter = repoFilter;
    }

    public String getPathFilter() {
        return pathFilter;
    }

    public void setPathFilter(String pathFilter) {
        this.pathFilter = pathFilter;
    }

    public DistributionCoordinates getDistributionCoordinates() {
        return distributionCoordinates;
    }

    public void setDistributionCoordinates(DistributionCoordinates distributionCoordinates) {
        this.distributionCoordinates = distributionCoordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DistributionRule)) return false;
        DistributionRule that = (DistributionRule) o;
        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
