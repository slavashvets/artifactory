package org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author gidis
 */
public class CriteriaRelativeDateElement extends DomainSensitiveParserElement {
    @Override
    protected ParserElement init() {
        return forward(quotes, provide(DynamicField.class), quotes, colon, openCurlyBrackets, quotedRelativeDateComparator,
                colon, fork(number,forward(quotes,number,quotes)), closedCurlyBrackets);
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}