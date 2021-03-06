package cz.cas.lib.core.index.solr.util;

import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.QueryStringHolder;

public class NestedCriteria extends Criteria implements QueryStringHolder {
    private static DefaultQueryParser parser = new DefaultQueryParser(new SimpleSolrMappingContext());

    private Criteria parentCriteria;
    private Criteria childrenCriteria;

    public NestedCriteria(Criteria parentCriteria, Criteria childrenCriteria) {
        this.parentCriteria = parentCriteria;
        this.childrenCriteria = childrenCriteria;
    }

    @Override
    public String getQueryString() {
        String parentQ = parser.createQueryStringFromNode(parentCriteria, null);
        String childrenQ = parser.createQueryStringFromNode(childrenCriteria, null);

        return "({!parent which=" + parentQ + " score=max v='" + childrenQ + "'})";
    }
}
