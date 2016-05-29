/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.storage.db.properties.service;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.properties.dao.DbPropertiesDao;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.codehaus.mojo.versions.ordering.MavenVersionComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * @author Gidi Shabat
 */
@Service
public class ArtifactoryDbPropertiesServiceImpl implements ArtifactoryDbPropertiesService {
    @Autowired
    private DbPropertiesDao dbPropertiesDao;

    @Override
    public void updateDbProperties(DbProperties dbProperties) {
        try {
            dbPropertiesDao.createProperties(dbProperties);
        } catch (SQLException e) {
            throw new StorageException("Failed to load db properties from database", e);
        }
    }

    @Override
    public DbProperties getDbProperties() {
        try {
            List<DbProperties> dbProperties = dbPropertiesDao.getProperties();
            Collections.sort(dbProperties, new DBPropertiesComparator());
            if (dbProperties.size() > 0) {
                return dbProperties.get(dbProperties.size() - 1);
            }
        }catch (Exception e){
            throw new StorageException("Failed to load db properties from database", e);
        }
        return null;
    }

    @Override
    public boolean isDbPropertiesTableExists() {
        try {
            return dbPropertiesDao.isDbPropertiesTableExists();
        } catch (SQLException e) {
            throw new StorageException("Failed to check if the  db_properties table exists in the database", e);
        }
    }

    private class DBPropertiesComparator implements Comparator<DbProperties> {

        private final MavenVersionComparator versionComparator;

        public DBPropertiesComparator() {
            versionComparator = new MavenVersionComparator();
        }

        @Override
        public int compare(DbProperties o1, DbProperties o2) {
            String artifactoryVersion1 = o1.getArtifactoryVersion();
            String artifactoryVersion2 = o2.getArtifactoryVersion();
            DefaultArtifactVersion version1 = new DefaultArtifactVersion(artifactoryVersion1);
            DefaultArtifactVersion version2 = new DefaultArtifactVersion(artifactoryVersion2);
            return versionComparator.compare(version1,version2);
        }
    }
}
