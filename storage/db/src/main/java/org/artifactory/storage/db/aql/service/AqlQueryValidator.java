package org.artifactory.storage.db.aql.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQueryElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.Criteria;
import org.artifactory.storage.db.aql.sql.builder.query.aql.SortDetails;
import org.artifactory.storage.db.aql.sql.builder.query.sql.AqlToSqlQueryBuilderException;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

import static org.artifactory.aql.model.AqlFieldEnum.*;

/**
 * @author Gidi Shabat
 */
public class AqlQueryValidator {
    private static final Logger log = LoggerFactory.getLogger(AqlQueryValidator.class);

    public void validate(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        // Assert that all the sort fields are unique
        assertSortFieldsAreUnique(aqlQuery);
        // Assert that all the sort fields exist in the result fields
        assertSortFieldsInResult(aqlQuery);
        // Assert that the result fields contains the repo, path and name fields for permissions needs
        assertMinimalResultFields(aqlQuery, permissionProvider);
        // Block property result filter for OSS
        blockPropertyResultFilterForOSS(aqlQuery, permissionProvider);
        // Block sorting for OSS
        blockSortingForOSS(aqlQuery, permissionProvider);
    }

    private void assertMinimalResultFields(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        if (permissionProvider.isAdmin()) {
            return;
        }
        if (!AqlDomainEnum.items.equals(aqlQuery.getDomain())) {
            return;
        }
        List<AqlFieldEnum> resultFields = Lists.transform(aqlQuery.getResultFields(), toAqlFieldEnum);
        List<AqlFieldEnum> minimalResultFields = Lists.newArrayList(itemRepo, itemPath, itemName);
        for (AqlFieldEnum sortField : minimalResultFields) {
            if (!resultFields.contains(sortField)) {
                throw new AqlToSqlQueryBuilderException(
                        "For permissions reasons AQL demands the following fields: repo, path and name.");
            }
        }
    }

    private void assertSortFieldsAreUnique(AqlQuery aqlQuery) {
        // Assert that all the sort fields are unique
        if (aqlQuery.getSort() != null && aqlQuery.getSort().getFields() != null) {
            List<AqlFieldEnum> fields = aqlQuery.getSort().getFields();
            if (fields.stream().distinct().count() != fields.size()) {
                throw new AqlToSqlQueryBuilderException(
                        "Duplicate fields, all the fields in the sort section should be unique.");
            }
        }
    }

    private void assertSortFieldsInResult(AqlQuery aqlQuery) {
        List<AqlFieldEnum> resultFields = Lists.transform(aqlQuery.getResultFields(), toAqlFieldEnum);
        if (aqlQuery.getSort() != null && aqlQuery.getSort().getFields() != null) {
            List<AqlFieldEnum> sortFields = aqlQuery.getSort().getFields();
            for (AqlFieldEnum sortField : sortFields) {
                if (!resultFields.contains(sortField)) {
                    throw new AqlToSqlQueryBuilderException(
                            "Only the result fields are allowed to use in the sort section.");
                }
            }
        }
    }

    private Function<DomainSensitiveField, AqlFieldEnum> toAqlFieldEnum = new Function<DomainSensitiveField, AqlFieldEnum>() {
        @Nullable
        @Override
        public AqlFieldEnum apply(@Nullable DomainSensitiveField domainSensitiveField) {
            if (domainSensitiveField != null) {
                return domainSensitiveField.getField();
            } else {
                return null;
            }
        }
    };

    private void blockSortingForOSS(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        if (permissionProvider.isOss()) {
            SortDetails sort = aqlQuery.getSort();
            if (sort != null && sort.getFields() != null && sort.getFields().size() > 0) {
                throw new AqlException("Sorting is not supported by AQL in the open source version\n");
            }
        }
    }

    private void blockPropertyResultFilterForOSS(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        if (permissionProvider.isOss()) {
            List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
            for (AqlQueryElement aqlElement : aqlElements) {
                if (aqlElement instanceof Criteria) {
                    SqlTable table1 = ((Criteria) aqlElement).getTable1();
                    if ((SqlTableEnum.node_props == table1.getTable() && table1.getId() < SqlTable.MINIMAL_DYNAMIC_TABLE_ID)||
                            (SqlTableEnum.build_props== table1.getTable() && table1.getId() < SqlTable.MINIMAL_DYNAMIC_TABLE_ID)||
                            (SqlTableEnum.module_props == table1.getTable() && table1.getId() < SqlTable.MINIMAL_DYNAMIC_TABLE_ID)) {
                        throw new AqlException(
                                "Filtering properties result is not supported by AQL in the open source version\n");
                    }
                }
            }
        }
    }
}
