package cz.cas.lib.core.index.solr;

import cz.cas.lib.core.domain.DomainObject;
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
 * Defines attribute {@link SolrDomainObject#id} of type {@link String}.
 * </p>
 * <p>
 * Always needs to have no arg constructor, otherwise exceptions will be thrown
 * in {@link SolrStore#save(DomainObject)}.
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SolrDomainObject that = (SolrDomainObject) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
