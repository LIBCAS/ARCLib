package cz.inqool.uas.index.solr;

import cz.inqool.uas.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

/**
 * Building block for dictionary like Solr entities.
 *
 * <p>
 *     To understand the dictionary concept {@link DictionaryObject }.
 * </p>
 */
@Getter
@Setter
public class SolrDictionaryObject extends SolrDatedObject {
    @Indexed
    @Field
    protected String name;

    @Indexed
    @Field
    protected Long order;

    @Indexed
    @Field
    protected Boolean active;
}
