package org.artifactory.storage.db.base.itest.dao;

import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.db.properties.dao.DbPropertiesDao;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.properties.service.ArtifactoryDbPropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Comparator;

import static org.testng.Assert.*;

/**
 * Date: 7/10/13 3:31 PM
 *
 * @author freds
 */
public class DbPropertiesDaoTest extends DbBaseTest {
    @Autowired
    private DbPropertiesDao dbPropertiesDao;

    @Autowired
    private ArtifactoryDbPropertiesService dbPropertiesService;

    @BeforeClass
    public void setup() {
        importSql("/sql/db-props.sql");
    }


    public void loadExistingProps() throws SQLException {
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        assertEquals(dbProperties.getInstallationDate(), 1349000000000L);
        assertEquals(dbProperties.getArtifactoryVersion(), "5-t");
        assertEquals(dbProperties.getArtifactoryRevision(), 12000);
        assertEquals(dbProperties.getArtifactoryRelease(), 1300000000000L);
    }

    private DbProperties getLatestProperties() throws SQLException {
        return dbPropertiesService.getDbProperties();
    }

    @Test(dependsOnMethods = {"loadExistingProps"})
    public void createNewLatestProps() throws SQLException {
        long now = System.currentTimeMillis() - 10000L;
        DbProperties dbTest = new DbProperties(now, "6-a", 1, 2L);
        dbPropertiesDao.createProperties(dbTest);
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        assertEquals(dbProperties.getInstallationDate(), now);
        assertEquals(dbProperties.getArtifactoryVersion(), "6-a");
        assertEquals(dbProperties.getArtifactoryRevision(), 1);
        assertEquals(dbProperties.getArtifactoryRelease(), 2L);
    }

    @Test(dependsOnMethods = {"createNewLatestProps"})
    public void createNewDevModeProps() throws SQLException {
        long now = System.currentTimeMillis();
        DbProperties dbTest = new DbProperties(now, "7-dev", -1, -3L);
        dbPropertiesDao.createProperties(dbTest);
        DbProperties dbProperties = getLatestProperties();
        assertNotNull(dbProperties);
        assertEquals(dbProperties.getInstallationDate(), now);
        assertEquals(dbProperties.getArtifactoryVersion(), "7-dev");
        assertEquals(dbProperties.getArtifactoryRevision(), 0);
        assertEquals(dbProperties.getArtifactoryRelease(), 0L);
    }



    private class CreationComparator implements Comparator<DbProperties> {
        @Override
        public int compare(DbProperties o1, DbProperties o2) {
            return (int) (o1.getInstallationDate() - o1.getInstallationDate());
        }
    }
}
