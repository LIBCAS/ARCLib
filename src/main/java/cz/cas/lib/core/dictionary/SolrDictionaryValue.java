package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrDictionaryObject;
import cz.cas.lib.core.index.solr.SolrReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

import java.time.LocalDateTime;

/**
 * Indexed representation of {@link DictionaryValue}.
 */
@Getter
@Setter
/**
 * todo remake, two solr references are not supported
 */
public class SolrDictionaryValue extends SolrDictionaryObject {
    @Field(child = true)
    @Indexed(readonly = true)
    protected SolrReference dictionary;

    @Field(child = true)
    @Indexed(readonly = true)
    protected SolrReference parent;

    @Field
    @Indexed(type = FieldType.FOLDING)
    protected String description;

    @Field
    @Indexed
    protected LocalDateTime validFrom;

    @Field
    @Indexed
    protected LocalDateTime validTo;

    @Field
    @Indexed
    protected String code;
}
