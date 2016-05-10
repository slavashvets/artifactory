package org.artifactory.storage.db.aql.parser.elements.high.level.basic.language;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.quotes;
import static org.artifactory.storage.db.aql.parser.AqlParser.relativeDateComparator;

/**
 * @author gidis
 */
public class QuotedRelativeDateComparator extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(quotes, relativeDateComparator, quotes);
    }
}