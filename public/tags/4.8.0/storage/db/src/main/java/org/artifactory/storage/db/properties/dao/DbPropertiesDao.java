package org.artifactory.storage.db.properties.dao;

import com.google.common.collect.Lists;
import com.sun.istack.internal.NotNull;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Date: 7/10/13 3:08 PM
 *
 * @author freds
 */
@Repository
public class DbPropertiesDao extends BaseDao {
    public static final String TABLE_NAME = "db_properties";

    @Autowired
    public DbPropertiesDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    @NotNull
    public List<DbProperties> getProperties() throws SQLException {
        List<DbProperties> result = Lists.newArrayList();
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT * FROM " + TABLE_NAME);
            while (rs.next()) {
                result.add(new DbProperties(rs.getLong(1), rs.getString(2),
                        zeroIfNull(rs.getInt(3)), zeroIfNull(rs.getLong(4))));
            }
        } finally {
            DbUtils.close(rs);
        }
        return result;
    }

    public boolean createProperties(DbProperties dbProperties) throws SQLException {
        int updateCount = jdbcHelper.executeUpdate("INSERT INTO " + TABLE_NAME +
                        " (installation_date, artifactory_version, artifactory_revision, artifactory_release)" +
                        " VALUES(?, ?, ?, ?)",
                dbProperties.getInstallationDate(), dbProperties.getArtifactoryVersion(),
                nullIfZeroOrNeg(dbProperties.getArtifactoryRevision()),
                nullIfZeroOrNeg(dbProperties.getArtifactoryRelease()));
        return updateCount == 1;
    }

    public boolean isDbPropertiesTableExists() throws SQLException {
        try (Connection con = jdbcHelper.getDataSource().getConnection()) {
            DatabaseMetaData metaData = con.getMetaData();
            return DbUtils.tableExists(metaData, DbPropertiesDao.TABLE_NAME);
        }
    }
}
