package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.AqlFieldResolver;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlVariable;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;

import java.util.List;

import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.build_props;
import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.node_props;

/**
 * @author Gidi Shabat
 */
public class ComplexPropertyCriteria extends Criteria {
    public ComplexPropertyCriteria(List<AqlDomainEnum> subDomains, AqlVariable variable1, SqlTable table1,
            String comparatorName, AqlVariable variable2, SqlTable table2, boolean mspOperator) {
        super(subDomains, variable1, table1, comparatorName, variable2, table2,mspOperator);
    }

    /**
     * Converts propertyCriteria to Sql criteria
     */
    @Override
    public String toSql(List<Object> params) throws AqlException {
        // Get both variable which are Values (this is property criteria)
        AqlVariable value1 = getVariable1();
        AqlVariable value2 = getVariable2();
        // Get both tables which are node_props tables (this is property criteria)
        SqlTable table1 = getTable1();
        // SqlTable table2 = getTable2();
        // update the Sql input param list
        // Get the ComparatorEnum
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(getComparatorName());
        return createSqlComplexPropertyCriteria(comparatorEnum, value1, table1, value2, params);
    }

    //
    public String createSqlComplexPropertyCriteria(AqlComparatorEnum comparatorEnum, AqlVariable variable1,
                                                   SqlTable table1,
                                                   AqlVariable variable2, List<Object> params) {
        AqlVariable key = AqlFieldResolver.resolve(AqlFieldEnum.propertyKey.signature);
        AqlVariable value = AqlFieldResolver.resolve(AqlFieldEnum.propertyValue.signature);
        Object param1 = resolveParam(variable1);
        Object param2 = resolveParam(variable2);
        String index1 = table1 != null ? table1.getAlias() : "";
        switch (comparatorEnum) {
            case equals: {
                return "(" + generateEqualsQuery(key, variable1, params, param1, index1) + " and " +
                        generateEqualsQuery(value, variable2, params, param2, index1) + ")";
            }
            case matches: {
                return "(" + generateEqualsQuery(key, variable1, params, param1, index1) + " and " +
                        generateMatchQuery(value, variable2, params, param2, index1) + ")";
            }
            case notMatches: {
                // In case that the query is inside MSP then use simple query without exist(...)
                if(  isMspOperator()){
                    return "(" + generateNotMatchQuery(key, variable1, params, param1, index1) + " or " +
                            generateNotMatchQuery(value, variable2, params, param2, index1) + ")";
                }else {
                    params.add(param1);
                    params.add(param2);
                    String tableName = table1.getTable().name();
                    String fieldName = node_props == table1.getTable() ? "node_id" :
                            build_props == table1.getTable() ? "build_id" : "module_id";
                    return "(" + index1 + fieldName + " is null or not exists (select 1 from "
                            + tableName + " where " + index1 + fieldName + " = " + fieldName + " and prop_key = " + toSql(
                            variable1) + " and prop_value like  " + toSql(variable2) + "))";
                }
            }
            case less: {
                return "(" + generateEqualsQuery(key, variable1, params, param1, index1) + " and " +
                        generateLessThanQuery(value, variable2, params, param2, index1) + ")";
            }
            case greater: {
                return "(" + generateEqualsQuery(key, variable1, params, param1, index1) + " and " +
                        generateGreaterThanQuery(value, variable2, params, param2, index1) + ")";
            }
            case greaterEquals: {
                return "(" + generateEqualsQuery(key, variable1, params, param1, index1) + " and " +
                        generateGreaterEqualQuery(value, variable2, params, param2, index1) + ")";
            }
            case lessEquals: {
                return "(" + generateEqualsQuery(key, variable1, params, param1, index1) + " and " +
                        generateLessEqualsQuery(value, variable2, params, param2, index1) + ")";
            }
            case notEquals: {
                // In case that the query is inside MSP then use simple query without exist(...)
                if(  isMspOperator()){
                    return "(" + generateNotEqualsQuery(key, variable1, params, param1, index1) + " or " +
                            generateNotEqualsQuery(value, variable2, params, param2, index1) + ")";
                }else {
                    params.add(param1);
                    params.add(param2);
                    String tableName = table1.getTable().name();
                    String fieldName = node_props == table1.getTable() ? "node_id" :
                            build_props == table1.getTable() ? "build_id" : "module_id";
                    return "(" + index1 + fieldName + " is null or not exists (select 1 from " + tableName + " where " +
                            index1 + fieldName + " = " + fieldName + " and prop_key = " + toSql(
                            variable1) + " and prop_value = " + toSql(variable2) + "))";
                }
            }
            default:
                throw new IllegalStateException("Should not reach to the point of code");
        }
    }
}
