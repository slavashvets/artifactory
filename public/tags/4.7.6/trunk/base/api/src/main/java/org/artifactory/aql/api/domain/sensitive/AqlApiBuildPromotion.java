package org.artifactory.aql.api.domain.sensitive;

import com.google.common.collect.Lists;
import org.artifactory.aql.api.internal.AqlApiDynamicFieldsDomains;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.result.rows.AqlBuildPromotion;

import java.util.ArrayList;

/**
 * @author gidis
 */
public class AqlApiBuildPromotion extends AqlBase<AqlApiBuildPromotion, AqlBuildPromotion> {

    public AqlApiBuildPromotion() {
        super(AqlBuildPromotion.class);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiBuildPromotion> created() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.buildPromotionCreated, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiBuildPromotion> createdBy() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.buildPromotionCreatedBy, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiBuildPromotion> comment() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.buildPromotionComment, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiBuildPromotion> status() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.buildPromotionStatus, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiBuildPromotion> repo() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.buildPromotionRepo, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiBuildPromotion> userName() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.buildPromotionUserName, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiBuildDynamicFieldsDomains<AqlApiBuildPromotion> build() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.buildPromotions, AqlDomainEnum.builds);
        return new AqlApiDynamicFieldsDomains.AqlApiBuildDynamicFieldsDomains(subDomains);
    }

    public static AqlApiBuildPromotion create() {
        return new AqlApiBuildPromotion();
    }
}