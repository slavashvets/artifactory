package org.artifactory.aql.result.rows;

import org.artifactory.aql.model.AqlDomainEnum;

import java.util.Date;

/**
 * @author gidis
 */
@QueryTypes(AqlDomainEnum.buildPromotions)
public interface AqlBuildPromotion  extends AqlRowResult {
    Date getBuildPromotionCreated();

    String getBuildPromotionCreatedBy();

    String getBuildPromotionUser();

    String getBuildPromotionComment();

    String getBuildPromotionStatus();

    String getBuildPromotionRepo();
}