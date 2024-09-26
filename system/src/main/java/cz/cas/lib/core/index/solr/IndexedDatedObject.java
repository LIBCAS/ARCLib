package cz.cas.lib.core.index.solr;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.core.index.Indexed;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

import java.util.Date;

/**
 * Building block for Solr entities, which want to track creation and update.
 * <p>
 * <p>
 * Provides attributes {@link IndexedDatedObject#created} and {@link IndexedDatedObject#updated}.
 * </p>
 * <p>
 * <p>
 * Unlike {@link DatedObject} there is no deleted attribute. Deleted entites are always removed from Solr.
 * Also unlike {@link DatedObject} the attributes are of {@link Date} type, because Solr does not
 * understand Java 8 Time classes.
 * </p>
 */

@Getter
@Setter
public abstract class IndexedDatedObject extends IndexedDomainObject {

    @Field
    @Indexed(type = IndexFieldType.DATE)
    protected Date created;

    @Field
    @Indexed(type = IndexFieldType.DATE)
    protected Date updated;
}
