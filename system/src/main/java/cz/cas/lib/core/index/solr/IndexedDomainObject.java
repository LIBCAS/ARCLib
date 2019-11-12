package cz.cas.lib.core.index.solr;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

/**
 * Basic building block for every Solr entity.
 *
 * <p>
 * Defines attribute {@link IndexedDomainObject#id} of type {@link String}.
 * </p>
 * <p>
 * Always needs to have no arg constructor, otherwise exceptions will be thrown
 * in {@link IndexedStore#save(DomainObject)}.
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public abstract class IndexedDomainObject {
    @Indexed(type = IndexFieldType.STRING)
    @Field
    protected String id;

    @Indexed(type = IndexFieldType.STRING)
    @Field(IndexQueryUtils.TYPE_FIELD)
    protected String type;

    public IndexedDomainObject(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexedDomainObject that = (IndexedDomainObject) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
