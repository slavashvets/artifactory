package org.artifactory.storage.db.aql.parser.elements.high.level.basic.language;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import java.util.List;

import static org.artifactory.aql.model.AqlDomainEnum.builds;
import static org.artifactory.aql.model.AqlDomainEnum.buildPromotions;
import static org.artifactory.storage.db.aql.parser.AqlParser.buildDomains;
import static org.artifactory.storage.db.aql.parser.AqlParser.dot;

/**
 * @author gidis
 */
public class BuildPromotionsDomainsElement extends LazyParserElement implements DomainProviderElement {
    @Override
    protected ParserElement init() {
        List<ParserElement> list = Lists.newArrayList();
        fillWithDomainFields(list);
        fillWithSubDomains(list);
        return fork(list.toArray(new ParserElement[list.size()]));
    }

    private void fillWithDomainFields(List<ParserElement> list) {
        list.add(new IncludeDomainElement(buildPromotions));
    }

    private void fillWithSubDomains(List<ParserElement> list) {
        list.add(forward(new InternalNameElement(builds.signatue),
                fork(new EmptyIncludeDomainElement(builds), forward(dot, buildDomains))));
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }

    @Override
    public AqlDomainEnum getDomain() {
        return buildPromotions;
    }
}
