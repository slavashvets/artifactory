/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
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

package org.artifactory.storage.db.security.itest.dao;

import org.artifactory.model.xstream.security.UserProperty;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.db.security.dao.UserPropertiesDao;
import org.fest.assertions.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Tests {@link UserPropertiesDao}.
 *
 * @author Yossi Shaul
 */
@Test
public class UserPropertiesDaoTest extends DbBaseTest {

    @Autowired
    private UserPropertiesDao dao;

    @BeforeClass
    public void setup() {
        importSql("/sql/user_properties.sql");
    }

    public void addPropertyToExistingUser() throws SQLException {
        dao.addUserPropertyById(1, "something", "good");    // user oferc
    }

    @Test(expectedExceptions = SQLIntegrityConstraintViolationException.class)
    public void addPropertyToNonExistingUser() throws SQLException {
        dao.addUserPropertyById(9911828, "wont", "work");
    }

    @Test(dependsOnMethods = "addPropertyToExistingUser")
    public void getUserProperties() throws SQLException {
        List<UserProperty> props = dao.getPropertiesForUser("oferc");
        Assertions.assertThat(props).hasSize(1);
        assertEquals(props.get(0).getPropKey(), "something");
        assertEquals(props.get(0).getPropValue(), "good");
    }

    public void getUserPropertiesNonExistingUser() throws SQLException {
        List<UserProperty> props = dao.getPropertiesForUser("nosuchuser");
        assertEquals(props.size(), 0);
    }
}
