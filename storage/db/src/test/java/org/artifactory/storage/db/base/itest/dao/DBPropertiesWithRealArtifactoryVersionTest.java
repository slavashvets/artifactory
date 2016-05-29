package org.artifactory.storage.db.base.itest.dao;

import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.db.properties.dao.DbPropertiesDao;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.properties.service.ArtifactoryDbPropertiesService;
import org.artifactory.storage.db.properties.utils.VersionPropertiesUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author gidis
 */
public class DBPropertiesWithRealArtifactoryVersionTest extends DbBaseTest {

    @Autowired
    private DbPropertiesDao dbPropertiesDao;

    @Autowired
    private ArtifactoryDbPropertiesService dbPropertiesService;

    @BeforeClass
    public void setup() {
        importSql("/sql/db-props-with-artifactory-version.sql");
    }

    private DbProperties getLatestProperties() throws SQLException {
        return dbPropertiesService.getDbProperties();
    }

    public void loadExistingProps() throws SQLException {
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        assertEquals(dbProperties.getInstallationDate(), 1349000000000L);
        assertEquals(dbProperties.getArtifactoryVersion(), "4.7.1");
        assertEquals(dbProperties.getArtifactoryRevision(), 12000);
        assertEquals(dbProperties.getArtifactoryRelease(), 1300000000000L);
    }

    @Test(dependsOnMethods = {"loadExistingProps"})
    public void createArtifactoryVersion() throws SQLException, InterruptedException {
        long now = System.currentTimeMillis() - 10000L;
        DbProperties dbTest = new DbProperties(now, "4.7.2", 1, 2L);
        dbPropertiesDao.createProperties(dbTest);
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        assertEquals(dbProperties.getInstallationDate(), now);
        assertEquals(dbProperties.getArtifactoryVersion(), "4.7.2");
        assertEquals(dbProperties.getArtifactoryRevision(), 1);
        assertEquals(dbProperties.getArtifactoryRelease(), 2L);
    }

    @Test(dependsOnMethods = {"createArtifactoryVersion"})
    public void latestVersion() throws SQLException, InterruptedException {
        long now = System.currentTimeMillis();
        ArtifactoryVersion current = ArtifactoryVersion.getCurrent();
        Thread.sleep(100);
        DbProperties fromVersion = VersionPropertiesUtils.createDbPropertiesFromVersion(new CompoundVersionDetails(
                current, current.getValue(), "" + current.getRevision(), now - 1000000L));
        dbPropertiesDao.createProperties(fromVersion);
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        assertTrue(now <= fromVersion.getInstallationDate());
        assertEquals(dbProperties.getInstallationDate(), fromVersion.getInstallationDate());
        assertEquals(dbProperties.getArtifactoryVersion(), current.getValue());
        assertEquals(dbProperties.getArtifactoryRevision(), 0);
        assertEquals(dbProperties.getArtifactoryRelease(), now - 1000000L);
    }

    @Test(dependsOnMethods = {"latestVersion"})
    public void downgrade() throws SQLException, InterruptedException {
        long now = System.currentTimeMillis() - 10000L;
        DbProperties dbTest = new DbProperties(now, "4.7.4", 1, 2L);
        dbPropertiesDao.createProperties(dbTest);
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        ArtifactoryVersion current = ArtifactoryVersion.getCurrent();
        DbProperties fromVersion = VersionPropertiesUtils.createDbPropertiesFromVersion(new CompoundVersionDetails(
                current, current.getValue(), "" + current.getRevision(), now - 1000000L));
        assertTrue(now <= fromVersion.getInstallationDate());
        assertEquals(dbProperties.getArtifactoryVersion(), current.getValue());
        assertEquals(dbProperties.getArtifactoryRevision(), 0);
    }
}
