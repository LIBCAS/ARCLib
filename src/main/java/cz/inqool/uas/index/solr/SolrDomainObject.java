package cz.inqool.uas.index.solr;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.IndexedStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

/**
 * Basic building block for every Solr entity.
 *
 * <p>
 *     Defines attribute {@link SolrDomainObject#id} of type {@link String}.
 * </p>
 * <p>
 *     Always needs to have no arg constructor, otherwise exceptions will be thrown
 *     in {@link IndexedStore#save(DomainObject)}.
 * </p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class SolrDomainObject {
    @Indexed
    @Field
    protected String id;

    @Indexed
    @Field
    protected final String type = this.getClass().getName();
}
