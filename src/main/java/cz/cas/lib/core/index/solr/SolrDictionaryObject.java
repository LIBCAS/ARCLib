package cz.cas.lib.core.index.solr;

import cz.cas.lib.core.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

/**
 * Building block for dictionary like Solr entities.
 *
 * <p>
 * To understand the dictionary concept {@link DictionaryObject }.
 * </p>
 */
@Getter
@Setter
public class SolrDictionaryObject extends SolrDatedObject {
    @Indexed(type = FieldType.FOLDING)
    @Field
    protected String name;

    @Indexed
    @Field
    protected Long order;

    @Indexed
    @Field
    protected Boolean active;
}
