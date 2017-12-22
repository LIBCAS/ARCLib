package cz.inqool.uas.index.solr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * Reference to another Solr entity or {@link Enum} value.
 *
 * <p>
 *     Objects in Solr can not be connected like JPA entities. Therefore we store only the referencing
 *     {@link SolrReference#id} and {@link SolrReference#name} attribute, which corresponds to label of that
 *     entity.
 * </p>
 * <p>
 *     In case of linking to {@link SolrDictionaryObject} the {@link SolrReference#name} is the
 *     {@link SolrDictionaryObject#name}.
 * </p>
 * <p>
 *     If used as reference to {@link Enum} then, the {@link SolrReference#id} becomes the {@link Enum#name} and
 *     {@link SolrReference#name} should be set to something the user see and filter on.
 * </p>
 */
@SolrDocument(solrCoreName = "gettingstarted")
@NoArgsConstructor
@Getter
@Setter
public class SolrReference extends SolrDomainObject {
    @Indexed
    @Field
    protected String name;

    public SolrReference(String id, String name) {
        super(id);
        this.name = name;
    }
}
