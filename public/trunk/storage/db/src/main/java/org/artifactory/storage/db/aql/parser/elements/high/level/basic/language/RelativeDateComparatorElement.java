package org.artifactory.storage.db.aql.parser.elements.high.level.basic.language;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlRelativeDateComparatorEnum;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import java.util.List;

/**
 * @author gidis
 */
public class RelativeDateComparatorElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        List<ParserElement> result = Lists.newArrayList();
        AqlRelativeDateComparatorEnum[] values = AqlRelativeDateComparatorEnum.values();
        for (AqlRelativeDateComparatorEnum value : values) {
            result.add(forward(new InternalNameElement(value.signature, true)));
        }
        ParserElement[] array = new ParserElement[result.size()];
        return fork(result.toArray(array));
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
