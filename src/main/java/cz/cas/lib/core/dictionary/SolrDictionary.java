package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrDictionaryObject;
import cz.cas.lib.core.index.solr.SolrReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

/**
 * Indexed representation of {@link Dictionary}.
 */
@Getter
@Setter
public class SolrDictionary extends SolrDictionaryObject {
    @Field
    @Indexed(type = FieldType.FOLDING)
    protected String description;

    @Field(child = true)
    @Indexed(readonly = true)
    protected SolrReference parent;

    @Field
    @Indexed
    protected String code;
}
