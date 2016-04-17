package org.artifactory.storage.db.aql.service;

import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBaseItem;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.fest.assertions.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author gidis
 */
public class AqlMspOrientedTest  extends DbBaseTest {
    @Autowired
    private AqlServiceImpl aqlService;

    @BeforeClass
    public void setup() {
        importSql("/sql/aql_msp.sql");
        ReflectionTestUtils.setField(aqlService, "permissionProvider", new AqlAbstractServiceTest.AdminPermissions());
    }

    @Test
    public void noneMsp() {
        // without $smp the result will include the item with the LGPL license because it has another property
        AqlEagerResult results = aqlService.executeQueryEager(
                "items.find({"+
                            "\"@license\":{\"$match\": \"*GPL*\"}, " +
                            "\"@license\":{\"$nmatch\": \"LGPL-V5*\"}" +
                        "})"
        );

        List<AqlBaseItem> items = results.getResults();
        Assertions.assertThat(items).hasSize(1);
        Assertions.assertThat(("ant-1.5.jar").equals(items.get(0).getName()));
    }

    @Test
    public void msp() {
        // without $smp the result will include the item with the LGPL license because it has another property
        AqlEagerResult results = aqlService.executeQueryEager(
                "items.find({\"$msp\": [" +
                            "{\"@license\":{\"$match\": \"*GPL*\"}}," +
                            "{\"@license\":{\"$ne\": \"LGPL-V5\"}}" +
                        "]})"
        );

        List<AqlBaseItem> items = results.getResults();
        Assertions.assertThat(items).hasSize(2);
        Assertions.assertThat(("ant-1.5.jar").equals(items.get(0).getName()));
    }

}
