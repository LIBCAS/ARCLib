package cz.cas.lib.core.index.nested;

import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDomainObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(collection = "testC")
public class IndexedChildEntity extends IndexedDomainObject {
    @Field
    @Indexed(type = IndexFieldType.TEXT, copyTo = {"attribute" + IndexField.STRING_SUFFIX, "attribute" + IndexField.SORT_SUFFIX})
    private String attribute;
}
